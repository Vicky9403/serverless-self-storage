/**
 * CloudStore — Frontend Configuration
 * 
 * Instructions for live AWS deployment:
 * 1. Set 'apiBaseUrl' to your deployed API Gateway endpoint (e.g., https://xxxxxxxxxx.execute-api.af-south-1.amazonaws.com/Prod)
 * 2. Set 'cognito' parameters from your AWS SAM stack outputs
 * 3. Set 'useMockApi' to false to route requests directly to AWS
 */
window.APP_CONFIG = {
    // API Gateway Base URL
    apiBaseUrl: localStorage.getItem('cloudstore_api_url') || '',

    // Amazon Cognito configuration
    cognito: {
        region: localStorage.getItem('cloudstore_cognito_region') || 'af-south-1',
        userPoolId: localStorage.getItem('cloudstore_user_pool_id') || '',
        clientId: localStorage.getItem('cloudstore_client_id') || ''
    },

    // When true, uses local mock storage for instant testing without AWS deployment
    // Auto-detects whether live endpoints are configured
    useMockApi: localStorage.getItem('cloudstore_mock_mode') !== 'false'
};

// Helper to save settings from the UI
window.updateConfig = function(apiBaseUrl, region, userPoolId, clientId, useMock) {
    localStorage.setItem('cloudstore_api_url', apiBaseUrl || '');
    localStorage.setItem('cloudstore_cognito_region', region || 'af-south-1');
    localStorage.setItem('cloudstore_user_pool_id', userPoolId || '');
    localStorage.setItem('cloudstore_client_id', clientId || '');
    localStorage.setItem('cloudstore_mock_mode', useMock ? 'true' : 'false');
    
    window.APP_CONFIG.apiBaseUrl = apiBaseUrl || '';
    window.APP_CONFIG.cognito.region = region || 'af-south-1';
    window.APP_CONFIG.cognito.userPoolId = userPoolId || '';
    window.APP_CONFIG.cognito.clientId = clientId || '';
    window.APP_CONFIG.useMockApi = useMock;
};
