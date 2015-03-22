/**
 *
 */
package com.github.federvieh.selma.assimillib;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.federvieh.selma.R;
import com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper.TextType;

/**
 * Adapter for showing all the texts in a lesson.
 *
 * @author frank
 */
public class AssimilShowLessonListAdapter extends ArrayAdapter<String> {
    private AssimilLesson lesson;
    private DisplayMode displayMode;

    private static Drawable dots = null;


    public AssimilShowLessonListAdapter(Context context, AssimilLesson lesson, ListTypes lt, DisplayMode displayMode) {
        super(context, R.layout.rowlayout, (lt == ListTypes.NO_TRANSLATE) ? lesson.getLessonList(displayMode) : lesson.getTextList(displayMode));
        this.lesson = lesson;
        this.displayMode = displayMode;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout_showlesson, parent, false);
        TextView textViewLeft = (TextView) rowView.findViewById(R.id.showLessonTextViewLeft);
        TextView textViewRight = (TextView) rowView.findViewById(R.id.showLessonTextViewRight);
        View devider = rowView.findViewById(R.id.showLessonDevider);
        switch (displayMode) {
            case LITERAL:
            case ORIGINAL_TEXT:
            case TRANSLATION: {
                String current = lesson.getTextList(displayMode)[position];
                textViewLeft.setText(current);
                textViewRight.setVisibility(View.GONE);
                devider.setVisibility(View.GONE);
                break;
            }
            case ORIGINAL_TRANSLATION: {
                String original = lesson.getTextList(DisplayMode.ORIGINAL_TEXT)[position];
                String translation = lesson.getTextList(DisplayMode.TRANSLATION)[position];
                textViewLeft.setText(original);
                textViewRight.setText(translation);
                break;
            }
            case ORIGINAL_LITERAL: {
                String original = lesson.getTextList(DisplayMode.ORIGINAL_TEXT)[position];
                String translation = lesson.getTextList(DisplayMode.LITERAL)[position];
                textViewLeft.setText(original);
                textViewRight.setText(translation);
                break;
            }
        }
        TextType textType = lesson.getTextTypeList()[position];
        switch (textType) {
            case TRANSLATE_HEADING:
                textViewLeft.setTextColor(context.getResources().getColor(R.color.DarkSlateGray));
                textViewRight.setTextColor(context.getResources().getColor(R.color.DarkSlateGray));
                //FALLTHROUGH
            case LESSONNUMBER:
            case HEADING:
                textViewLeft.setTextSize(18);
                textViewLeft.setTypeface(null, Typeface.ITALIC);
                textViewRight.setTextSize(18);
                textViewRight.setTypeface(null, Typeface.ITALIC);
                break;
            case TRANSLATE:
                textViewLeft.setTextColor(context.getResources().getColor(R.color.DarkSlateGray));
                textViewRight.setTextColor(context.getResources().getColor(R.color.DarkSlateGray));
                //FALLTHROUGH
            case NORMAL:
                textViewLeft.setTextSize(16);
                textViewRight.setTextSize(16);
                break;
        }
        if ((LessonPlayer.getTrackNumber(getContext()) == position) && (LessonPlayer.getLesson(getContext()).getHeader().equals(lesson.getHeader()))) {
            textViewLeft.setTypeface(null, Typeface.BOLD | ((textViewLeft.getTypeface() != null) ? textViewLeft.getTypeface().getStyle() : 0));
            textViewRight.setTypeface(null, Typeface.BOLD | ((textViewRight.getTypeface() != null) ? textViewRight.getTypeface().getStyle() : 0));
        }
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        if (dots == null) {
            Drawable d = getContext().getResources().getDrawable(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);
            PorterDuff.Mode tintMode = PorterDuff.Mode.MULTIPLY;
            PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.GRAY, tintMode);
            d.setColorFilter(filter);
            dots = d;
        }
        imageView.setImageDrawable(dots);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(getContext(), imageView);
                //Inflating the Popup using xml file
                popup.getMenuInflater()
                        .inflate(R.menu.translate, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.add_translation:
                            case R.id.add_original_text:
                            case R.id.add_literal: {
                                final EditText translateEditText = new EditText(getContext());
                                final Context ctxt = getContext();
                                int title = R.string.change_translation;
                                DisplayMode dm = DisplayMode.TRANSLATION;
                                DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        lesson.setTranslateText(position, translateEditText.getText().toString(), ctxt);
                                    }
                                };
                                if (item.getItemId() == R.id.add_literal) {
                                    title = R.string.change_literal;
                                    dm = DisplayMode.LITERAL;
                                    ocl = new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            lesson.setLiteralText(position, translateEditText.getText().toString(), ctxt);
                                        }
                                    };
                                }
                                if (item.getItemId() == R.id.add_original_text) {
                                    title = R.string.change_original_text;
                                    dm = DisplayMode.ORIGINAL_TEXT;
                                    ocl = new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            lesson.setOriginalText(position, translateEditText.getText().toString(), ctxt);
                                        }
                                    };
                                }
                                AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
                                builder.setTitle(title);
                                builder.setMessage(lesson.getTextList(DisplayMode.ORIGINAL_TEXT)[position]);
                                translateEditText.setText(lesson.getTextList(dm)[position]);
                                builder.setView(translateEditText);
                                builder.setPositiveButton(ctxt.getText(R.string.ok), ocl);
                                builder.setNegativeButton(ctxt.getText(R.string.cancel), new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Nothing to do
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                return true;
                            }
                            default:
                                return false;
                        }
                    }
                });

                popup.show(); //showing popup menu

            }
        });
//		AssimilOnClickListener assimilOnClickListener = new AssimilOnClickListener(current, context, position, lt);
//		textView.setOnClickListener(assimilOnClickListener);
        //imageView.setVisibility(View.GONE);

        return rowView;
    }
}
