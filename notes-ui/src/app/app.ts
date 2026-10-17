import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Note, NotesApi } from './notes-api';

@Component({
  selector: 'app-root',
  imports: [DatePipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private api = inject(NotesApi);

  notes = signal<Note[]>([]);
  tags = signal<string[]>([]);
  activeTag = signal<string | null>(null);
  error = signal<string | null>(null);

  title = signal('');
  content = signal('');
  tagText = signal('');

  constructor() {
    this.refresh();
  }

  refresh() {
    this.api.list(this.activeTag() ?? undefined).subscribe({
      next: (n) => {
        this.notes.set(n);
        this.error.set(null);
      },
      error: () => this.error.set('Cannot reach the notes API. Check that notes-api is running.'),
    });
    this.api.tags().subscribe({ next: (t) => this.tags.set(t), error: () => {} });
  }

  filter(tag: string | null) {
    this.activeTag.set(tag);
    this.refresh();
  }

  add() {
    const title = this.title().trim();
    if (!title) return;
    const tags = this.tagText()
      .split(',')
      .map((t) => t.trim().toLowerCase())
      .filter(Boolean);
    this.api.create({ title, content: this.content().trim(), tags: [...new Set(tags)] }).subscribe({
      next: () => {
        this.title.set('');
        this.content.set('');
        this.tagText.set('');
        this.refresh();
      },
      error: () => this.error.set('Could not save the note. Check the title and tags, then try again.'),
    });
  }

  remove(note: Note) {
    this.api.remove(note.id).subscribe({
      next: () => this.refresh(),
      error: () => this.error.set('Could not delete the note.'),
    });
  }

  val(e: Event) {
    return (e.target as HTMLInputElement | HTMLTextAreaElement).value;
  }
}
