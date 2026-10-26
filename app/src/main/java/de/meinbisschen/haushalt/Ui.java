package de.meinbisschen.haushalt;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Gestaltung: Salbeigrün, Beige und warme Naturtöne – alles an einem Ort. */
public final class Ui {

    public static final int BG = 0xFFF5F0E6;          // Beige
    public static final int SURFACE = 0xFFFFFCF6;     // warmes Weiß
    public static final int SAGE = 0xFF8A9A7B;        // Salbeigrün
    public static final int SAGE_DARK = 0xFF5F7052;
    public static final int SAGE_LIGHT = 0xFFE4EADC;
    public static final int SAND = 0xFFEDE4D3;
    public static final int SAND_DARK = 0xFFD9C9AC;
    public static final int TEXT = 0xFF3E3A33;
    public static final int MUTED = 0xFF8C8475;
    public static final int LINE = 0xFFE6DDCB;
    public static final int CLAY = 0xFFB98668;        // warmes Terrakotta für „Löschen“

    private Ui() { }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, float radiusPx, int strokeColor, int strokePx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        if (strokePx > 0) g.setStroke(strokePx, strokeColor);
        return g;
    }

    public static Drawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x22000000), content, null);
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setLineSpacing(0, 1.15f);
        return tv;
    }

    public static TextView title(Context c, String s) {
        TextView tv = text(c, s, 26, TEXT, true);
        tv.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        return tv;
    }

    public static TextView section(Context c, String s) {
        TextView tv = text(c, s, 15, SAGE_DARK, true);
        tv.setPadding(dp(c, 4), dp(c, 18), 0, dp(c, 8));
        return tv;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        l.setBackground(rounded(SURFACE, dp(c, 20), LINE, dp(c, 1)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 10);
        l.setLayoutParams(lp);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    /** Pillenförmiger Knopf. primary = gefülltes Salbeigrün. */
    public static TextView button(Context c, String label, boolean primary, View.OnClickListener l) {
        TextView b = text(c, label, 15, primary ? 0xFFFFFFFF : SAGE_DARK, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        GradientDrawable bg = primary
                ? rounded(SAGE, dp(c, 24), 0, 0)
                : rounded(SURFACE, dp(c, 24), SAND_DARK, dp(c, 1));
        b.setBackground(ripple(bg));
        b.setOnClickListener(l);
        b.setMinHeight(dp(c, 48));
        return b;
    }

    /** Kleiner, ruhiger Text-Knopf. */
    public static TextView smallButton(Context c, String label, int color, View.OnClickListener l) {
        TextView b = text(c, label, 14, color, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        b.setBackground(ripple(rounded(0x00000000, dp(c, 16), 0, 0)));
        b.setOnClickListener(l);
        b.setMinHeight(dp(c, 40));
        return b;
    }

    /** Auswahl-Chip (z. B. Wochentag oder Energie-Modus). */
    public static TextView chip(Context c, String label, boolean selected, View.OnClickListener l) {
        TextView b = text(c, label, 14, selected ? 0xFFFFFFFF : TEXT, selected);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10));
        GradientDrawable bg = selected
                ? rounded(SAGE, dp(c, 18), 0, 0)
                : rounded(SAND, dp(c, 18), 0, 0);
        b.setBackground(ripple(bg));
        b.setOnClickListener(l);
        b.setMinHeight(dp(c, 44));
        return b;
    }

    public static LinearLayout.LayoutParams weight(Context c, float w, int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
        lp.leftMargin = dp(c, marginDp);
        lp.rightMargin = dp(c, marginDp);
        return lp;
    }

    public static LinearLayout.LayoutParams wrap(Context c, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topDp);
        return lp;
    }

    public static LinearLayout.LayoutParams full(Context c, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topDp);
        return lp;
    }

    public static View space(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    public static View flex(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }
}
