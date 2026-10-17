import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Note {
  id: number;
  title: string;
  content: string;
  tags: string[];
  createdAt: string;
}
export interface NoteInput {
  title: string;
  content: string;
  tags: string[];
}

// Relative URLs: in dev the Angular proxy forwards /api, on OpenShift nginx does.
@Injectable({ providedIn: 'root' })
export class NotesApi {
  private http = inject(HttpClient);

  list(tag?: string) {
    return this.http.get<Note[]>('/api/notes', { params: tag ? { tag } : {} });
  }
  tags() {
    return this.http.get<string[]>('/api/tags');
  }
  create(note: NoteInput) {
    return this.http.post<Note>('/api/notes', note);
  }
  remove(id: number) {
    return this.http.delete<void>(`/api/notes/${id}`);
  }
}
