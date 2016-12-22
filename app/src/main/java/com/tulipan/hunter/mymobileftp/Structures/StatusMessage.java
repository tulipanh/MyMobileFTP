package com.tulipan.hunter.mymobileftp.Structures;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Hunter on 12/13/2016.
 */
public class StatusMessage {
    public final String message;
    public final Integer type;
    public final String time;

    public StatusMessage(String m, Integer t) {
        message = m;
        type = t;
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss", Locale.US);
        time = sdf.format(date);
    }
}
