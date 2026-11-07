package com.tcms.automation;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
public class DemoAppController {

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Demo Login App</title>
    <style>
        body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f0f2f5; }
        .card { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 320px; }
        .form-group { margin-bottom: 16px; }
        label { display: block; margin-bottom: 6px; font-weight: bold; }
        input { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
        button { width: 100%; padding: 10px; background: #0066cc; color: white; border: none; border-radius: 4px; font-weight: bold; cursor: pointer; }
        button:hover { background: #0052a3; }
        .welcome { color: #008800; font-weight: bold; margin-top: 16px; }
        .error { color: #cc0000; font-weight: bold; margin-top: 16px; }
    </style>
</head>
<body>
    <div class="card">
        <h2>Demo Application</h2>
        <form id="login-form" onsubmit="handleLogin(event)">
            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" data-testid="username-input" autocomplete="off" />
            </div>
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" data-testid="password-input" autocomplete="off" />
            </div>
            <button type="submit" id="login-btn" aria-label="Sign In">Sign In</button>
        </form>
        <div id="result-container">
            <h1 id="welcome" class="welcome" style="display: none;">Welcome, admin!</h1>
            <div id="error" class="error" style="display: none;">Invalid username or password</div>
        </div>
    </div>
    <script>
        function handleLogin(e) {
            e.preventDefault();
            const u = document.getElementById('username').value;
            const p = document.getElementById('password').value;
            const welcome = document.getElementById('welcome');
            const error = document.getElementById('error');
            if (u === 'admin' && p === 'secret') {
                welcome.style.display = 'block';
                error.style.display = 'none';
            } else {
                welcome.style.display = 'none';
                error.style.display = 'block';
            }
        }
    </script>
</body>
</html>
""";
    }
}
