package com.example.notes.ui;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.R;
import com.example.notes.data.Note;

public class NotesAdapter extends ListAdapter<Note, NotesAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    private static final DiffUtil.ItemCallback<Note> DIFF = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final OnNoteClickListener listener;

    public NotesAdapter(OnNoteClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView previewView;
        private final TextView dateView;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.note_title);
            previewView = itemView.findViewById(R.id.note_preview);
            dateView = itemView.findViewById(R.id.note_date);
        }

        void bind(Note note, OnNoteClickListener listener) {
            boolean hasTitle = note.title != null && !note.title.isEmpty();
            titleView.setText(hasTitle ? note.title : itemView.getContext().getString(R.string.untitled_note));
            previewView.setText(note.content);
            previewView.setVisibility(note.content == null || note.content.isEmpty() ? View.GONE : View.VISIBLE);
            dateView.setText(DateUtils.formatDateTime(itemView.getContext(), note.updatedAt,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
            itemView.setOnClickListener(v -> listener.onNoteClick(note));
        }
    }
}
