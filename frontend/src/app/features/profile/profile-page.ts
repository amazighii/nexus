import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { SessionStore } from '../../core/state/session.store';
import { ToastService } from '../../core/services/toast.service';
import { MediaService } from '../../core/services/media.service';
import { UserService } from '../../core/services/user.service';
import { extractApiErrorMessage } from '../../core/utils/http-error';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';

const MAX_IMAGE_BYTES = 2 * 1024 * 1024;

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, SpinnerComponent],
  template: `
    <div class="container">
      <div class="surface card">
        <div class="head">
          <h1>Profile</h1>
          @if (session.loadingMe()) {
            <app-spinner />
          }
        </div>

        @if (session.loadingMe() && !session.me()) {
          <div class="center"><app-spinner /></div>
        } @else if (!session.isAuthed()) {
          <p class="muted">Your profile will appear here after login.</p>
        } @else if (!session.me()) {
          <p class="muted">Could not load your profile.</p>
          <button class="btn" type="button" (click)="reloadProfile()">Retry</button>
        } @else {
          <div class="grid">
            <div class="row">
              <div class="label">Name</div>
              <div class="value">{{ session.me()!.firstName }} {{ session.me()!.lastName }}</div>
            </div>
            <div class="row">
              <div class="label">Email</div>
              <div class="value">{{ session.me()!.email }}</div>
            </div>
            <div class="row">
              <div class="label">Role</div>
              <div class="value">{{ session.me()!.role }}</div>
            </div>
            <div class="row">
              <div class="label">User ID</div>
              <div class="value mono">{{ session.me()!.id }}</div>
            </div>
          </div>

          <div class="section">
            <h2>Avatar</h2>
            <p class="muted">
              Sellers can upload an avatar image (max 2 MB). The backend stores an avatar URL on your profile.
            </p>

            <div class="avatar">
              @if (previewUrl()) {
                <img [src]="previewUrl()!" alt="Avatar preview" />
              } @else if (session.me()!.avatarUrl) {
                <img [src]="session.me()!.avatarUrl!" alt="Avatar" />
              } @else {
                <div class="avatar__ph">No avatar</div>
              }
            </div>

            @if (!session.isSeller()) {
              <div class="note">Avatar upload is restricted to SELLER accounts by the Media Service.</div>
            } @else {
              <div class="actions">
                <input class="file" type="file" accept="image/*" (change)="onFile($event)" />
                <button class="btn" type="button" (click)="upload()" [disabled]="!file() || uploading()">
                  @if (uploading()) { Uploading… } @else { Upload avatar }
                </button>
              </div>
              @if (uploadError()) {
                <div class="err">{{ uploadError() }}</div>
              }
            }

            
          </div>

        }
      </div>
    </div>
  `,
  styles: [
    `
      .card {
        padding: 20px;
      }
      .head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
      }
      .center {
        padding: 24px 0;
        display: grid;
        place-items: center;
      }
      h1 {
        margin: 0;
        letter-spacing: -0.03em;
      }
      .grid {
        margin-top: 16px;
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
      }
      .row {
        border: 1px solid var(--border);
        border-radius: 14px;
        padding: 12px;
        background: var(--surface-2);
      }
      .label {
        color: var(--muted);
        font-size: 12px;
      }
      .value {
        margin-top: 6px;
        font-weight: 650;
      }
      .mono {
        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
          monospace;
        font-size: 12px;
      }
      .section {
        margin-top: 18px;
        padding-top: 18px;
        border-top: 1px solid var(--border);
      }
      h2 {
        margin: 0;
        font-size: 16px;
      }
      h3 {
        margin: 0 0 10px;
        font-size: 14px;
      }
      .avatar {
        margin-top: 12px;
        width: 92px;
        height: 92px;
        border-radius: 999px;
        overflow: hidden;
        border: 1px solid var(--border);
        background: var(--surface-2);
        display: grid;
        place-items: center;
      }
      .avatar img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      .avatar__ph {
        color: var(--muted);
        font-size: 12px;
      }
      .actions {
        margin-top: 12px;
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;
      }
      .btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 40px;
        padding: 0 14px;
        border-radius: 12px;
        border: 1px solid rgba(15, 118, 110, 0.25);
        background: var(--primary);
        color: white;
        font-weight: 650;
        cursor: pointer;
      }
      .btn:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
      .btn--ghost {
        background: transparent;
        color: var(--text);
        border-color: var(--border);
      }
      .btn--ghost:hover {
        background: rgba(17, 24, 39, 0.04);
      }
      .note {
        margin-top: 12px;
        padding: 12px;
        border-radius: 14px;
        background: rgba(17, 24, 39, 0.03);
        border: 1px solid var(--border);
        color: var(--muted);
        font-size: 13px;
      }
      .err {
        margin-top: 10px;
        color: var(--danger);
        font-size: 12px;
      }
      .urlForm {
        margin-top: 16px;
        display: grid;
        gap: 10px;
      }
      input[type='url'] {
        width: 100%;
        border-radius: 12px;
        border: 1px solid var(--border);
        padding: 10px 12px;
        background: var(--surface);
        font: inherit;
        margin-top: 8px;
      }
      @media (max-width: 760px) {
        .grid,
        .urlForm {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class ProfilePage {
  readonly session = inject(SessionStore);
  private readonly toast = inject(ToastService);
  private readonly media = inject(MediaService);
  private readonly users = inject(UserService);
  private readonly fb = inject(FormBuilder);

  readonly file = signal<File | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly uploading = signal(false);
  readonly uploadError = signal<string | null>(null);
  readonly savingUrl = signal(false);

  readonly urlForm = this.fb.nonNullable.group({
    avatarUrl: ['', [Validators.required]],
  });

  constructor() {
    const current = this.session.me()?.avatarUrl ?? '';
    this.urlForm.patchValue({ avatarUrl: current });
    if (this.session.isAuthed() && !this.session.me()) void this.reloadProfile();
  }

  async reloadProfile() {
    try {
      await this.session.refreshMe();
      const current = this.session.me()?.avatarUrl ?? '';
      this.urlForm.patchValue({ avatarUrl: current });
    } catch (e) {
      this.toast.show('error', 'Could not load profile', this.extractError(e));
    }
  }

  onFile(ev: Event) {
    this.uploadError.set(null);
    const input = ev.target as HTMLInputElement;
    const f = input.files?.[0] ?? null;
    if (!f) return;
    if (!f.type.startsWith('image/')) {
      this.uploadError.set('Only image files are allowed.');
      return;
    }
    if (f.size > MAX_IMAGE_BYTES) {
      this.uploadError.set('File too large. Max 2 MB.');
      return;
    }
    this.file.set(f);
    const url = URL.createObjectURL(f);
    this.previewUrl.set(url);
  }

  upload() {
    const f = this.file();
    if (!f) return;
    this.uploadError.set(null);
    this.uploading.set(true);

    this.media.uploadProfileImage(f).subscribe({
      next: async (evt) => {
        if (evt.state !== 'done' || !evt.data) return;
        try {
          await this.users.updateAvatar({ avatarUrl: evt.data.url });
          await this.session.refreshMe();
          this.toast.show('success', 'Avatar updated');
        } catch (e) {
          this.uploadError.set(this.extractError(e));
        } finally {
          this.uploading.set(false);
        }
      },
      error: (e) => {
        this.uploadError.set(this.extractError(e));
        this.uploading.set(false);
      },
    });
  }

  async saveUrl() {
    if (this.urlForm.invalid || this.savingUrl()) return;
    try {
      this.savingUrl.set(true);
      await this.users.updateAvatar({ avatarUrl: this.urlForm.getRawValue().avatarUrl });
      await this.session.refreshMe();
      this.toast.show('success', 'Avatar URL saved');
    } catch (e) {
      this.toast.show('error', 'Could not save avatar', this.extractError(e));
    } finally {
      this.savingUrl.set(false);
    }
  }

  private extractError(e: unknown): string {
    return extractApiErrorMessage(e, 'Request failed');
  }
}
