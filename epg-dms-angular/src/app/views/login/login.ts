import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  authError = '';

  async handleLogin(): Promise<void> {
    try {
      this.authError = '';
      await this.auth.login(this.email, this.password);
      await this.router.navigate(['/']);
    } catch (error: any) {
      this.authError = error.error?.error;
    }
  }
}
