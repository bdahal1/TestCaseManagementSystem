import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface User {
    userId: string;
    fullName: string;
    photoURL?: string;
    roleList: string[];
}

interface AuthContextType {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    login: (token: string, user: User) => void;
    logout: () => void;
    loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedToken = localStorage.getItem('accessToken');
        const storedUser = localStorage.getItem('user');

        if (storedToken && storedUser) {
            setToken(storedToken);
            setUser(JSON.parse(storedUser));
        }
        setLoading(false);
    }, []);

    const login = (newToken: string, newUser: User) => {
        setToken(newToken);
        setUser(newUser);
        localStorage.setItem('accessToken', newToken);
        localStorage.setItem('user', JSON.stringify(newUser));

        // Update axios default header if needed, though interceptor handles it
    };

    const logout = () => {
        setToken(null);
        setUser(null);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');
        // Clear legacy keys if they exist
        localStorage.removeItem('authToken');
        localStorage.removeItem('userId');
        localStorage.removeItem('roleList');
        localStorage.removeItem('fullName');
        localStorage.removeItem('isLoggedIn');
    };

    const isAuthenticated = !!token;

    useEffect(() => {
        const handleLogoutEvent = () => {
            logout();
        };

        window.addEventListener('auth:logout', handleLogoutEvent);

        return () => {
            window.removeEventListener('auth:logout', handleLogoutEvent);
        };
    }, []);

    return (
        <AuthContext.Provider value={{ user, token, isAuthenticated, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
