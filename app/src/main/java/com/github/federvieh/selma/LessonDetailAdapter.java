/*
 * Copyright (C) 2016 Frank Oltmanns (frank.oltmanns+selma(at)gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.federvieh.selma;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter for showing all the texts in a lesson.
 *
 * @author frank
 */
public class LessonDetailAdapter extends RecyclerView.Adapter<LessonDetailAdapter.ViewHolder> {
    private final String notYetTranslated;
    private DisplayMode displayMode;
    private final Cursor cursor;
    private final int[] idxText = new int[DisplayMode.values().length];
    private final int idxTextType;
    private final int idxLessonId;
    private final ListTypes lt;
    private Drawable dots;
    private long lessonId;

    public LessonDetailAdapter(Cursor cursor, DisplayMode displayMode, ListTypes lt, Context ctxt) {
        Log.d(this.getClass().getSimpleName(), "created with cursor of size " + cursor.getCount());
        this.cursor = cursor;
        this.displayMode = displayMode;
        for (DisplayMode dm : DisplayMode.values()) {
            if (dm == DisplayMode.LITERAL) {
                this.idxText[dm.ordinal()] = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTLIT);
            } else if (dm == DisplayMode.ORIGINAL_TEXT) {
                this.idxText[dm.ordinal()] = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXT);
            } else if (dm == DisplayMode.TRANSLATION) {
                this.idxText[dm.ordinal()] = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTRANS);
            } else if (dm == DisplayMode.ORIGINAL_LITERAL) {
                /* silently ignore */
            } else if (dm == DisplayMode.ORIGINAL_TRANSLATION) {
                /* silently ignore */
            } else {
                throw new IllegalArgumentException("The code is not complete!");
            }
        }
        this.idxTextType = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTYPE);
        this.idxLessonId = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID);
        cursor.moveToFirst();
        this.lessonId = cursor.getLong(this.idxLessonId);
        this.lt = lt;
        this.notYetTranslated = ctxt.getString(R.string.not_yet_translated);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rowlayout_lesson_detail, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        TextView textViewLeft = holder.getTextViewLeft();
        TextView textViewRight = holder.getTextViewRight();
        View devider = holder.getDevider();

        cursor.moveToPosition(position);
        switch (displayMode) {
            case LITERAL:
            case ORIGINAL_TEXT:
            case TRANSLATION: {
                String current = cursor.getString(idxText[displayMode.ordinal()]);
                if (TextUtils.isEmpty(current)) {
                    current = notYetTranslated;
                }
                textViewLeft.setText(current);
                textViewRight.setVisibility(View.GONE);
                devider.setVisibility(View.GONE);
                break;
            }
            case ORIGINAL_TRANSLATION: {
                String original = cursor.getString(idxText[DisplayMode.ORIGINAL_TEXT.ordinal()]);
                String translation = cursor.getString(idxText[DisplayMode.TRANSLATION.ordinal()]);
                if (TextUtils.isEmpty(translation)) {
                    translation = notYetTranslated;
                }
                textViewLeft.setText(original);
                textViewRight.setText(translation);

                textViewRight.setVisibility(View.VISIBLE);
                devider.setVisibility(View.VISIBLE);
                break;
            }
            case ORIGINAL_LITERAL: {
                String original = cursor.getString(idxText[DisplayMode.ORIGINAL_TEXT.ordinal()]);
                String translation = cursor.getString(idxText[DisplayMode.LITERAL.ordinal()]);
                if (TextUtils.isEmpty(translation)) {
                    translation = notYetTranslated;
                }
                textViewLeft.setText(original);
                textViewRight.setText(translation);

                textViewRight.setVisibility(View.VISIBLE);
                devider.setVisibility(View.VISIBLE);
                break;
            }
        }
        SelmaSQLiteHelper2.TextType textType = SelmaSQLiteHelper2.TextType.values()[cursor.getInt(idxTextType)];
        textViewLeft.setTypeface(null, Typeface.NORMAL);
        textViewRight.setTypeface(null, Typeface.NORMAL);
        textViewLeft.setTextColor(textViewLeft.getResources().getColor(android.R.color.black));
        textViewRight.setTextColor(textViewRight.getResources().getColor(android.R.color.black));
        switch (textType) {
            case TRANSLATE_HEADING:
                textViewLeft.setTextColor(textViewLeft.getResources().getColor(android.support.v7.appcompat.R.color.material_blue_grey_800));
                textViewRight.setTextColor(textViewRight.getResources().getColor(android.support.v7.appcompat.R.color.material_blue_grey_800));
                //FALLTHROUGH
            case LESSONNUMBER:
            case HEADING:
                textViewLeft.setTextSize(18);
                textViewLeft.setTypeface(null, Typeface.ITALIC);
                textViewRight.setTextSize(18);
                textViewRight.setTypeface(null, Typeface.ITALIC);
                break;
            case TRANSLATE:
                textViewLeft.setTextColor(textViewLeft.getResources().getColor(android.support.v7.appcompat.R.color.material_blue_grey_800));
                textViewRight.setTextColor(textViewRight.getResources().getColor(android.support.v7.appcompat.R.color.material_blue_grey_800));
                //FALLTHROUGH
            case NORMAL:
                textViewLeft.setTextSize(16);
                textViewRight.setTextSize(16);
                break;
        }
        //FIXME: Highlight currently played item
        holder.setLessonId(lessonId);
        if ((LessonPlayer.getTrackNumber(textViewLeft.getContext()) == position) &&
                (LessonPlayer.getLesson(textViewLeft.getContext()).getId() == lessonId)) {
            textViewLeft.setTypeface(null, Typeface.BOLD | ((textViewLeft.getTypeface() != null) ? textViewLeft.getTypeface().getStyle() : 0));
            textViewRight.setTypeface(null, Typeface.BOLD | ((textViewRight.getTypeface() != null) ? textViewRight.getTypeface().getStyle() : 0));
        }
        final ImageView imageView = (ImageView) holder.getIcon();
        if (dots == null) {
            Drawable d = imageView.getContext().getResources().getDrawable(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);
            PorterDuff.Mode tintMode = PorterDuff.Mode.MULTIPLY;
            PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.GRAY, tintMode);
            d.setColorFilter(filter);
            dots = d;
        }
        imageView.setImageDrawable(dots);
        //FIXME: Add menu to lessontexts
//        imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //Creating the instance of PopupMenu
//                PopupMenu popup = new PopupMenu(imageView.getContext(), imageView);
//                //Inflating the Popup using xml file
//                popup.getMenuInflater()
//                        .inflate(R.menu.translate, popup.getMenu());
//
//                //registering popup with OnMenuItemClickListener
//                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//                    public boolean onMenuItemClick(MenuItem item) {
//                        switch (item.getItemId()) {
//                            case R.id.add_translation:
//                            case R.id.add_original_text:
//                            case R.id.add_literal: {
//                                final EditText translateEditText = new EditText(imageView.getContext());
//                                final Context ctxt = imageView.getContext();
//                                int title = R.string.change_translation;
//                                DisplayMode dm = DisplayMode.TRANSLATION;
//                                DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
//
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        lesson.setTranslateText(position, translateEditText.getText().toString(), ctxt);
//                                    }
//                                };
//                                if (item.getItemId() == R.id.add_literal) {
//                                    title = R.string.change_literal;
//                                    dm = DisplayMode.LITERAL;
//                                    ocl = new DialogInterface.OnClickListener() {
//
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            lesson.setLiteralText(position, translateEditText.getText().toString(), ctxt);
//                                        }
//                                    };
//                                }
//                                if (item.getItemId() == R.id.add_original_text) {
//                                    title = R.string.change_original_text;
//                                    dm = DisplayMode.ORIGINAL_TEXT;
//                                    ocl = new DialogInterface.OnClickListener() {
//
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            lesson.setOriginalText(position, translateEditText.getText().toString(), ctxt);
//                                        }
//                                    };
//                                }
//                                AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
//                                builder.setTitle(title);
//                                builder.setMessage(lesson.getTextList(DisplayMode.ORIGINAL_TEXT)[position]);
//                                translateEditText.setText(lesson.getTextList(dm)[position]);
//                                builder.setView(translateEditText);
//                                builder.setPositiveButton(ctxt.getText(R.string.ok), ocl);
//                                builder.setNegativeButton(ctxt.getText(R.string.cancel), new DialogInterface.OnClickListener() {
//
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        // Nothing to do
//                                    }
//                                });
//                                AlertDialog dialog = builder.create();
//                                dialog.show();
//                                return true;
//                            }
//                            default:
//                                return false;
//                        }
//                    }
//                });
//
//                popup.show(); //showing popup menu
//
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public long getLessonId() {
        return lessonId;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View icon;
        TextView textViewLeft;
        TextView textViewRight;
        View devider;
        private long lessonId = -1;

        public ViewHolder(final View itemView) {
            super(itemView);
            textViewLeft = (TextView) itemView.findViewById(R.id.showLessonTextViewLeft);
            textViewRight = (TextView) itemView.findViewById(R.id.showLessonTextViewRight);
            devider = itemView.findViewById(R.id.showLessonDevider);
            icon = itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //FIXME: play/pause track
                    int currentTrack = LessonPlayer.getTrackNumber(itemView.getContext());
                    Lesson currentLesson = LessonPlayer.getLesson(itemView.getContext());
                    long currentLessonId = -1;
                    if (currentLesson != null) {
                        currentLessonId = currentLesson.getId();
                    }
                    if (currentLessonId != lessonId || currentTrack != getAdapterPosition()) {
                        Lesson lesson;
                        if (currentLessonId == lessonId) {
                            lesson = LessonPlayer.getLesson(itemView.getContext());
                        } else {
                            lesson = new Lesson(lessonId, itemView.getContext(), lt);
                        }
                        LessonPlayer.play(lesson, getAdapterPosition(), false, itemView.getContext());
                    } else if (LessonPlayer.isPlaying()) {
                        //We're playing the track that has been clicked -> pause
                        LessonPlayer.stopPlaying(itemView.getContext(), true);
                    } else {
                        //We're pausing on the track that has been clicked -> resume
                        LessonPlayer.play(LessonPlayer.getLesson(itemView.getContext()), getAdapterPosition(), true, itemView.getContext());
                    }
                }
            });
        }

        public TextView getTextViewLeft() {
            return textViewLeft;
        }

        public TextView getTextViewRight() {
            return textViewRight;
        }

        public View getDevider() {
            return devider;
        }

        public View getIcon() {
            return icon;
        }

        public void setLessonId(long id) {
            lessonId = id;
        }
    }
}
