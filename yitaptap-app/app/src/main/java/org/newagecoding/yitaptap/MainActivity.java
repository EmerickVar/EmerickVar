package org.newagecoding.yitaptap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText interval;
    private EditText duration;
    private EditText repetitions;
    private EditText delay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 11, 16));
        getWindow().setNavigationBarColor(Color.rgb(11, 11, 16));
        setContentView(buildUi());
        loadConfig();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(11, 11, 16));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));

        TextView title = new TextView(this);
        title.setText("YiTapTap");
        title.setTextSize(30f);
        title.setTextColor(Color.WHITE);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Auto-tap configurable mediante gestos de accesibilidad de Android.");
        subtitle.setTextSize(15f);
        subtitle.setTextColor(Color.rgb(190, 190, 205));
        subtitle.setPadding(0, dp(8), 0, dp(22));
        root.addView(subtitle);

        interval = numericField(root, "Intervalo entre toques (ms)");
        duration = numericField(root, "Duración de cada toque (ms)");
        repetitions = numericField(root, "Número de toques (0 = infinito)");
        delay = numericField(root, "Retraso antes de iniciar (ms)");

        root.addView(actionButton("Guardar configuración", v -> {
            if (saveConfig()) toast("Configuración guardada");
        }));

        root.addView(actionButton("1. Activar servicio de accesibilidad", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        root.addView(actionButton("2. Mostrar objetivo y controles flotantes", v -> {
            if (!saveConfig()) return;
            TapAccessibilityService service = TapAccessibilityService.getInstance();
            if (service == null) {
                toast("Activa primero YiTapTap en Accesibilidad");
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else {
                service.showOverlay();
                toast("Arrastra el objetivo ◎ al punto que quieras tocar");
            }
        }));

        root.addView(actionButton("Iniciar", v -> {
            if (!saveConfig()) return;
            TapAccessibilityService service = TapAccessibilityService.getInstance();
            if (service == null) toast("El servicio de accesibilidad no está activo");
            else {
                service.showOverlay();
                service.startTapping();
            }
        }));

        root.addView(actionButton("Detener", v -> {
            TapAccessibilityService service = TapAccessibilityService.getInstance();
            if (service != null) service.stopTapping();
        }));

        root.addView(actionButton("Ocultar controles flotantes", v -> {
            TapAccessibilityService service = TapAccessibilityService.getInstance();
            if (service != null) service.hideOverlay();
        }));

        TextView help = new TextView(this);
        help.setText("Uso: activa YiTapTap en Accesibilidad, muestra el objetivo, arrástralo al punto deseado y pulsa INICIAR. El objetivo deja de interceptar toques mientras la automatización está ejecutándose.");
        help.setTextSize(13f);
        help.setTextColor(Color.rgb(155, 155, 170));
        help.setPadding(0, dp(22), 0, 0);
        root.addView(help);

        scroll.addView(root);
        return scroll;
    }

    private EditText numericField(LinearLayout parent, String labelText) {
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(14f);
        label.setTextColor(Color.rgb(220, 220, 230));
        label.setPadding(0, dp(9), 0, dp(5));
        parent.addView(label);

        EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.GRAY);
        field.setSingleLine(true);
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        field.setBackgroundColor(Color.rgb(31, 31, 42));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(10);
        parent.addView(field, lp);
        return field;
    }

    private Button actionButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setGravity(Gravity.CENTER);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(8);
        button.setLayoutParams(lp);
        return button;
    }

    private void loadConfig() {
        TapSettings.Config c = TapSettings.load(this);
        interval.setText(Long.toString(c.intervalMs));
        duration.setText(Long.toString(c.durationMs));
        repetitions.setText(Integer.toString(c.repetitions));
        delay.setText(Long.toString(c.startDelayMs));
    }

    private boolean saveConfig() {
        try {
            TapSettings.Config c = new TapSettings.Config(
                    Long.parseLong(interval.getText().toString()),
                    Long.parseLong(duration.getText().toString()),
                    Integer.parseInt(repetitions.getText().toString()),
                    Long.parseLong(delay.getText().toString())
            );
            TapSettings.save(this, c);
            return true;
        } catch (NumberFormatException ex) {
            toast("Revisa los valores numéricos");
            return false;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
