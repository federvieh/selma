package com.github.federvieh.selma;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class AdjustPauseDialogFragment extends DialogFragment implements SeekBar.OnSeekBarChangeListener {
    private SeekBar seekBar;
    private int currentDelay;
    private TextView textViewCurrentPause;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.title_adjust_pause)
                .setPositiveButton(R.string.OK,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                LessonPlayer.setDelay(currentDelay, getContext());
                                dialog.dismiss();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                );

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.pausebar, null);
        seekBar = (SeekBar) view.findViewById(R.id.sbDelay);
        textViewCurrentPause = (TextView) view.findViewById(R.id.textViewCurrentDelay);

        // set this instance as callback for editor action
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setProgress(LessonPlayer.getDelay(getContext()));

        b.setView(view);

        return b.create();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        currentDelay = progress;
        textViewCurrentPause.setText(progress + "%");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}