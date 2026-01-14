import { createTheme, alpha } from '@mui/material/styles';

// Premium Palette
const primaryColor = '#6366f1'; // Indigo 500
const secondaryColor = '#8b5cf6'; // Violet 500
const successColor = '#10b981'; // Emerald 500
const warningColor = '#f59e0b'; // Amber 500
const errorColor = '#ef4444'; // Red 500
const infoColor = '#06b6d4'; // Cyan 500
const backgroundDefault = '#f3f4f6'; // Gray 100 (slightly cooler/lighter)
const paperColor = '#ffffff';

const theme = createTheme({
    palette: {
        mode: 'light',
        primary: {
            main: primaryColor,
            light: '#818cf8',
            dark: '#4f46e5',
            contrastText: '#ffffff',
        },
        secondary: {
            main: secondaryColor,
            light: '#a78bfa',
            dark: '#7c3aed',
            contrastText: '#ffffff',
        },
        background: {
            default: backgroundDefault,
            paper: paperColor,
        },
        text: {
            primary: '#111827', // Gray 900
            secondary: '#4b5563', // Gray 600
        },
        success: { main: successColor },
        warning: { main: warningColor },
        error: { main: errorColor },
        info: { main: infoColor },
    },
    typography: {
        fontFamily: "'Inter', 'Roboto', 'Helvetica', 'Arial', sans-serif",
        h1: { fontWeight: 700, fontSize: '2.5rem', letterSpacing: '-0.02em' },
        h2: { fontWeight: 700, fontSize: '2rem', letterSpacing: '-0.01em' },
        h3: { fontWeight: 600, fontSize: '1.75rem', letterSpacing: '-0.01em' },
        h4: { fontWeight: 600, fontSize: '1.5rem' },
        h5: { fontWeight: 600, fontSize: '1.25rem' },
        h6: { fontWeight: 600, fontSize: '1rem' },
        subtitle1: { fontSize: '1rem', fontWeight: 500, color: '#4b5563' },
        subtitle2: { fontSize: '0.875rem', fontWeight: 500, color: '#6b7280' },
        body1: { fontSize: '0.95rem', lineHeight: 1.6 },
        body2: { fontSize: '0.875rem', lineHeight: 1.57 },
        button: { fontWeight: 600, textTransform: 'none' },
    },
    shape: {
        borderRadius: 12,
    },
    components: {
        MuiCssBaseline: {
            styleOverrides: {
                body: {
                    backgroundColor: backgroundDefault,
                    scrollbarColor: "#cbd5e1 transparent",
                    "&::-webkit-scrollbar, & *::-webkit-scrollbar": {
                        backgroundColor: "transparent",
                        width: 8,
                        height: 8,
                    },
                    "&::-webkit-scrollbar-thumb, & *::-webkit-scrollbar-thumb": {
                        borderRadius: 8,
                        backgroundColor: "#cbd5e1",
                        minHeight: 24,
                    },
                    "&::-webkit-scrollbar-thumb:focus, & *::-webkit-scrollbar-thumb:focus": {
                        backgroundColor: "#94a3b8",
                    },
                },
            },
        },
        MuiButton: {
            styleOverrides: {
                root: {
                    borderRadius: '10px',
                    padding: '8px 20px',
                    fontSize: '0.9rem',
                    boxShadow: 'none',
                    transition: 'all 0.2s ease-in-out',
                    '&:hover': {
                        transform: 'translateY(-1px)',
                        boxShadow: '0 4px 12px rgba(99, 102, 241, 0.2)',
                    },
                },
                containedPrimary: {
                    background: `linear-gradient(135deg, ${primaryColor}, ${secondaryColor})`,
                    '&:hover': {
                        background: `linear-gradient(135deg, ${secondaryColor}, ${primaryColor})`,
                    },
                },
            },
        },
        MuiCard: {
            styleOverrides: {
                root: {
                    borderRadius: '16px',
                    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
                    border: '1px solid rgba(255, 255, 255, 0.5)',
                    backgroundImage: 'none',
                    backdropFilter: 'blur(20px)',
                    transition: 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
                    '&:hover': {
                        // transform: 'translateY(-2px)',
                        boxShadow: '0 12px 30px rgba(0, 0, 0, 0.08)',
                    },
                },
            },
        },
        MuiPaper: {
            styleOverrides: {
                root: {
                    backgroundImage: 'none',
                },
                elevation1: {
                    boxShadow: '0 2px 10px rgba(0, 0, 0, 0.03)',
                },
            },
        },
        MuiTextField: {
            styleOverrides: {
                root: {
                    '& .MuiOutlinedInput-root': {
                        borderRadius: '10px',
                        backgroundColor: alpha('#fff', 0.8),
                        transition: 'all 0.2s',
                        '&:hover': {
                            backgroundColor: '#fff',
                        },
                        '&.Mui-focused': {
                            backgroundColor: '#fff',
                            boxShadow: `0 0 0 4px ${alpha(primaryColor, 0.1)}`,
                        },
                    },
                },
            },
        },
        MuiTableCell: {
            styleOverrides: {
                root: {
                    padding: '16px 24px',
                    borderBottom: '1px solid #f1f5f9',
                },
                head: {
                    fontWeight: 600,
                    backgroundColor: '#f8fafc',
                    color: '#64748b',
                    textTransform: 'uppercase',
                    fontSize: '0.75rem',
                    letterSpacing: '0.05em',
                },
            },
        },
        MuiTableRow: {
            styleOverrides: {
                root: {
                    '&:last-child td, &:last-child th': {
                        border: 0,
                    },
                    transition: 'background-color 0.1s',
                    '&:hover': {
                        backgroundColor: '#f8fafc',
                    },
                },
            },
        },
        MuiDialog: {
            styleOverrides: {
                paper: {
                    borderRadius: '20px',
                    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
                },
            },
        },
        MuiDrawer: {
            styleOverrides: {
                paper: {
                    backgroundColor: '#ffffff',
                    borderRight: '1px solid #f1f5f9',
                }
            }
        }
    },
});

export default theme;
