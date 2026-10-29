/**
 * CloudStore — API Client (AWS API Gateway & Local Mock Mode)
 */
class ApiService {
    constructor() {
        this.initMockData();
    }

    initMockData() {
        if (!localStorage.getItem('cloudstore_mock_units')) {
            const initialUnits = [
                { id: "unit-101", name: "Mini Locker A-1", size: "Small", price: 45.0, available: true },
                { id: "unit-102", name: "Mini Locker A-2", size: "Small", price: 45.0, available: false },
                { id: "unit-201", name: "Standard Unit B-1", size: "Medium", price: 85.0, available: true },
                { id: "unit-202", name: "Standard Unit B-2", size: "Medium", price: 85.0, available: true },
                { id: "unit-301", name: "Large Warehouse C-1", size: "Large", price: 150.0, available: false },
                { id: "unit-302", name: "Climate Control C-2", size: "Large", price: 175.0, available: true }
            ];
            localStorage.setItem('cloudstore_mock_units', JSON.stringify(initialUnits));
        }

        if (!localStorage.getItem('cloudstore_mock_bookings')) {
            localStorage.setItem('cloudstore_mock_bookings', JSON.stringify([]));
        }

        if (!localStorage.getItem('cloudstore_mock_files')) {
            localStorage.setItem('cloudstore_mock_files', JSON.stringify([]));
        }
    }

    isMock() {
        return window.APP_CONFIG.useMockApi || !window.APP_CONFIG.apiBaseUrl;
    }

    async request(path, options = {}) {
        const idToken = window.authService.getIdToken();
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        if (idToken) {
            headers['Authorization'] = idToken; // API Gateway Cognito Authorizer accepts the ID token
        }

        const baseUrl = window.APP_CONFIG.apiBaseUrl.replace(/\/$/, '');
        const response = await fetch(`${baseUrl}${path}`, {
            ...options,
            headers
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            const errorMsg = data.error || data.message || `Request failed with status ${response.status}`;
            throw new Error(errorMsg);
        }

        return data;
    }

    // ================= STORAGE UNITS =================

    async getUnits() {
        if (this.isMock()) {
            await new Promise(r => setTimeout(r, 200)); // slight latency for realism
            return JSON.parse(localStorage.getItem('cloudstore_mock_units') || '[]');
        }
        return this.request('/units', { method: 'GET' });
    }

    // ================= BOOKINGS =================

    async getBookings() {
        if (this.isMock()) {
            await new Promise(r => setTimeout(r, 200));
            const user = window.authService.getUser();
            const allBookings = JSON.parse(localStorage.getItem('cloudstore_mock_bookings') || '[]');
            if (!user) return [];
            return allBookings.filter(b => b.userId === user.sub);
        }
        return this.request('/bookings', { method: 'GET' });
    }

    async createBooking(unitId) {
        if (this.isMock()) {
            await new Promise(r => setTimeout(r, 300));
            const user = window.authService.getUser();
            if (!user) throw new Error('Unauthorized');

            const units = JSON.parse(localStorage.getItem('cloudstore_mock_units') || '[]');
            const unit = units.find(u => u.id === unitId);
            if (!unit) throw new Error('Storage unit not found');
            if (!unit.available) throw new Error('Storage unit is not available');

            // Mark unit unavailable
            unit.available = false;
            localStorage.setItem('cloudstore_mock_units', JSON.stringify(units));

            // Create booking
            const booking = {
                id: 'bk-' + Math.random().toString(36).substring(2, 9),
                userId: user.sub,
                unitId: unitId,
                bookingDate: new Date().toISOString(),
                status: 'ACTIVE'
            };

            const bookings = JSON.parse(localStorage.getItem('cloudstore_mock_bookings') || '[]');
            bookings.push(booking);
            localStorage.setItem('cloudstore_mock_bookings', JSON.stringify(bookings));

            return booking;
        }

        return this.request('/bookings', {
            method: 'POST',
            body: JSON.stringify({ unitId })
        });
    }

    async cancelBooking(bookingId) {
        if (this.isMock()) {
            await new Promise(r => setTimeout(r, 300));
            const user = window.authService.getUser();
            if (!user) throw new Error('Unauthorized');

            const bookings = JSON.parse(localStorage.getItem('cloudstore_mock_bookings') || '[]');
            const booking = bookings.find(b => b.id === bookingId);
            if (!booking) throw new Error('Booking not found');
            if (booking.userId !== user.sub) throw new Error('Forbidden');
            if (booking.status === 'CANCELLED') throw new Error('Booking is already cancelled');

            booking.status = 'CANCELLED';
            localStorage.setItem('cloudstore_mock_bookings', JSON.stringify(bookings));

            // Restore unit availability
            const units = JSON.parse(localStorage.getItem('cloudstore_mock_units') || '[]');
            const unit = units.find(u => u.id === booking.unitId);
            if (unit) {
                unit.available = true;
                localStorage.setItem('cloudstore_mock_units', JSON.stringify(units));
            }

            return { message: 'Booking cancelled successfully' };
        }

        return this.request(`/bookings/${bookingId}`, {
            method: 'DELETE'
        });
    }

    // ================= FILE STORAGE =================

    async getUploadUrl(fileName, contentType) {
        if (this.isMock()) {
            await new Promise(r => setTimeout(r, 150));
            const user = window.authService.getUser();
            const safeName = fileName.replace(/^.*[\\\/]/, '');
            return {
                fileName: safeName,
                fileKey: `${user.sub}/${safeName}`,
                uploadUrl: 'mock-s3-upload-url'
            };
        }

        return this.request('/files/upload', {
            method: 'POST',
            body: JSON.stringify({ fileName, contentType })
        });
    }

    async uploadFile(file, onProgress) {
        const { uploadUrl, fileKey, fileName } = await this.getUploadUrl(file.name, file.type);

        if (this.isMock()) {
            // Simulate direct S3 upload with progress
            for (let percent = 10; percent <= 100; percent += 20) {
                await new Promise(r => setTimeout(r, 80));
                if (onProgress) onProgress(percent);
            }

            // Save file record locally
            const user = window.authService.getUser();
            const files = JSON.parse(localStorage.getItem('cloudstore_mock_files') || '[]');
            const existingIndex = files.findIndex(f => f.userId === user.sub && f.fileName === fileName);
            
            const fileEntry = {
                fileName,
                fileKey,
                size: file.size,
                contentType: file.type || 'application/octet-stream',
                uploadedAt: new Date().toISOString(),
                userId: user.sub
            };

            if (existingIndex >= 0) {
                files[existingIndex] = fileEntry;
            } else {
                files.unshift(fileEntry);
            }
            localStorage.setItem('cloudstore_mock_files', JSON.stringify(files));

            return fileEntry;
        }

        // Live S3 upload using pre-signed PUT URL
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open('PUT', uploadUrl, true);
            if (file.type) {
                xhr.setRequestHeader('Content-Type', file.type);
            }

            xhr.upload.onprogress = (e) => {
                if (e.lengthComputable && onProgress) {
                    const percent = Math.round((e.loaded / e.total) * 100);
                    onProgress(percent);
                }
            };

            xhr.onload = () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    // Record file locally so user can browse their files
                    this.recordUserFile(fileName, file.size, file.type);
                    resolve({ fileName, fileKey });
                } else {
                    reject(new Error(`S3 upload failed with status ${xhr.status}`));
                }
            };

            xhr.onerror = () => reject(new Error('Network error uploading file to S3'));
            xhr.send(file);
        });
    }

    async getDownloadUrl(fileName) {
        if (this.isMock()) {
            await new Promise(r => setTimeout(r, 150));
            // In mock mode, generate a dummy text download URL
            const blob = new Blob([`Simulated CloudStore file download for: ${fileName}\nUploaded via CloudStore Web Dashboard.`], { type: 'text/plain' });
            return {
                fileName,
                fileKey: `mock/${fileName}`,
                downloadUrl: URL.createObjectURL(blob)
            };
        }

        return this.request(`/files/${encodeURIComponent(fileName)}`, {
            method: 'GET'
        });
    }

    recordUserFile(fileName, size, contentType) {
        const user = window.authService.getUser();
        if (!user) return;
        const key = `cloudstore_files_${user.sub}`;
        const files = JSON.parse(localStorage.getItem(key) || '[]');
        const existingIndex = files.findIndex(f => f.fileName === fileName);
        const item = {
            fileName,
            size,
            contentType,
            uploadedAt: new Date().toISOString()
        };
        if (existingIndex >= 0) {
            files[existingIndex] = item;
        } else {
            files.unshift(item);
        }
        localStorage.setItem(key, JSON.stringify(files));
    }

    getUserFiles() {
        const user = window.authService.getUser();
        if (!user) return [];
        if (this.isMock()) {
            const files = JSON.parse(localStorage.getItem('cloudstore_mock_files') || '[]');
            return files.filter(f => f.userId === user.sub);
        }
        const key = `cloudstore_files_${user.sub}`;
        return JSON.parse(localStorage.getItem(key) || '[]');
    }
}

window.apiService = new ApiService();
