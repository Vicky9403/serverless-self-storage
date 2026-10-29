/**
 * CloudStore — Authentication Module (Amazon Cognito & Local Mock Mode)
 */
class AuthService {
    constructor() {
        this.STORAGE_KEY = 'cloudstore_auth_session';
        this.listeners = [];
        this.session = this.loadSession();
    }

    loadSession() {
        try {
            const raw = localStorage.getItem(this.STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            console.error('Failed to parse auth session:', e);
            return null;
        }
    }

    saveSession(session) {
        this.session = session;
        if (session) {
            localStorage.setItem(this.STORAGE_KEY, JSON.stringify(session));
        } else {
            localStorage.removeItem(this.STORAGE_KEY);
        }
        this.notifyListeners();
    }

    onAuthStateChanged(callback) {
        this.listeners.push(callback);
        callback(this.getUser());
    }

    notifyListeners() {
        const user = this.getUser();
        this.listeners.forEach(cb => cb(user));
    }

    isAuthenticated() {
        return !!(this.session && this.session.idToken);
    }

    getUser() {
        if (!this.session) return null;
        return {
            sub: this.session.sub,
            email: this.session.email,
            isMock: !!this.session.isMock
        };
    }

    getIdToken() {
        return this.session ? this.session.idToken : null;
    }

    // Call Cognito Identity Provider REST API directly without heavy dependencies
    async callCognito(target, payload) {
        const region = window.APP_CONFIG.cognito.region || 'af-south-1';
        const url = `https://cognito-idp.${region}.amazonaws.com/`;

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-amz-json-1.1',
                'X-Amz-Target': `AWSCognitoIdentityProviderService.${target}`
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if (!response.ok) {
            const errorType = data.__type || 'CognitoError';
            const message = data.message || data.Message || 'Authentication failed';
            throw new Error(`${errorType}: ${message}`);
        }

        return data;
    }

    async signUp(email, password) {
        const isMock = window.APP_CONFIG.useMockApi || !window.APP_CONFIG.cognito.clientId;

        if (isMock) {
            // Simulated registration
            const users = JSON.parse(localStorage.getItem('cloudstore_mock_users') || '{}');
            users[email.toLowerCase()] = {
                email,
                password,
                confirmed: true, // auto-confirm in mock mode for instant testing
                sub: 'mock-user-' + Math.random().toString(36).substring(2, 9)
            };
            localStorage.setItem('cloudstore_mock_users', JSON.stringify(users));
            return { userConfirmed: true, userSub: users[email.toLowerCase()].sub };
        }

        const clientId = window.APP_CONFIG.cognito.clientId;
        const result = await this.callCognito('SignUp', {
            ClientId: clientId,
            Username: email,
            Password: password,
            UserAttributes: [
                { Name: 'email', Value: email }
            ]
        });

        return {
            userConfirmed: result.UserConfirmed,
            userSub: result.UserSub
        };
    }

    async confirmSignUp(email, code) {
        const isMock = window.APP_CONFIG.useMockApi || !window.APP_CONFIG.cognito.clientId;

        if (isMock) {
            return true;
        }

        const clientId = window.APP_CONFIG.cognito.clientId;
        await this.callCognito('ConfirmSignUp', {
            ClientId: clientId,
            Username: email,
            ConfirmationCode: code
        });

        return true;
    }

    async signIn(email, password) {
        const isMock = window.APP_CONFIG.useMockApi || !window.APP_CONFIG.cognito.clientId;

        if (isMock) {
            // Simulated login
            const users = JSON.parse(localStorage.getItem('cloudstore_mock_users') || '{}');
            let user = users[email.toLowerCase()];

            if (!user) {
                // Auto-create demo user for smooth testing
                user = {
                    email,
                    password,
                    confirmed: true,
                    sub: 'user-' + Math.random().toString(36).substring(2, 8)
                };
                users[email.toLowerCase()] = user;
                localStorage.setItem('cloudstore_mock_users', JSON.stringify(users));
            }

            const session = {
                idToken: 'mock-jwt-id-token-' + btoa(JSON.stringify({ sub: user.sub, email: user.email })),
                accessToken: 'mock-jwt-access-token',
                sub: user.sub,
                email: user.email,
                isMock: true,
                expiresAt: Date.now() + 3600 * 1000
            };

            this.saveSession(session);
            return session;
        }

        const clientId = window.APP_CONFIG.cognito.clientId;
        const result = await this.callCognito('InitiateAuth', {
            AuthFlow: 'USER_PASSWORD_AUTH',
            ClientId: clientId,
            AuthParameters: {
                USERNAME: email,
                PASSWORD: password
            }
        });

        const authResult = result.AuthenticationResult;
        if (!authResult) {
            throw new Error('Authentication challenge required or unexpected response');
        }

        // Parse claims from IdToken (middle segment of JWT)
        let sub = '';
        try {
            const base64Url = authResult.IdToken.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const claims = JSON.parse(decodeURIComponent(escape(window.atob(base64))));
            sub = claims.sub || '';
        } catch (e) {
            console.warn('Could not decode token claims:', e);
        }

        const session = {
            idToken: authResult.IdToken,
            accessToken: authResult.AccessToken,
            refreshToken: authResult.RefreshToken,
            sub: sub,
            email: email,
            isMock: false,
            expiresAt: Date.now() + (authResult.ExpiresIn || 3600) * 1000
        };

        this.saveSession(session);
        return session;
    }

    signOut() {
        this.saveSession(null);
    }
}

window.authService = new AuthService();
