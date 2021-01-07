package com.example.blurdialogex.lib;

/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.StateSet;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import androidx.annotation.UiThread;

public class Theme {

    public static final String DEFAULT_BACKGROUND_SLUG = "d";
    public static final String THEME_BACKGROUND_SLUG = "t";
    public static final String COLOR_BACKGROUND_SLUG = "c";

    public static class MessageDrawable extends Drawable {

        private LinearGradient gradientShader;
        private int currentBackgroundHeight;
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint selectedPaint;
        private int currentColor;
        private int currentGradientColor;

        private RectF rect = new RectF();
        private Matrix matrix = new Matrix();
        private int currentType;
        private boolean isSelected;
        private Path path;

        private Rect backupRect = new Rect();

        private final boolean isOut;

        private int topY;
        private boolean isTopNear;
        private boolean isBottomNear;

        private int[] currentShadowDrawableRadius = new int[]{-1, -1, -1, -1};
        private Drawable[] shadowDrawable = new Drawable[4];
        private int[] shadowDrawableColor = new int[]{0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff};

        private int[][] currentBackgroundDrawableRadius = new int[][]{
                {-1, -1, -1, -1},
                {-1, -1, -1, -1}};
        private Drawable[][] backgroundDrawable = new Drawable[2][4];
        private int[][] backgroundDrawableColor = new int[][]{
                {0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff},
                {0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff}};

        public static final int TYPE_TEXT = 0;
        public static final int TYPE_MEDIA = 1;
        public static final int TYPE_PREVIEW = 2;

        private int alpha;

        public MessageDrawable(int type, boolean out, boolean selected) {
            super();
            isOut = out;
            currentType = type;
            isSelected = selected;
            path = new Path();
            selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            alpha = 255;
        }

        public boolean hasGradient() {
            return gradientShader != null && shouldDrawGradientIcons;
        }

        public LinearGradient getGradientShader() {
            return gradientShader;
        }

        public Matrix getMatrix() {
            return matrix;
        }

        protected int getColor(String key) {
            return Color.GREEN;
        }

        protected Integer getCurrentColor(String key) {
            return Theme.currentColors.get(key);
        }

        public void setTop(int top, int backgroundHeight, boolean topNear, boolean bottomNear) {
            int color;
            Integer gradientColor;
            if (isOut) {
                color = getColor(isSelected ? key_chat_outBubbleSelected : key_chat_outBubble);
                gradientColor = getCurrentColor(key_chat_outBubbleGradient);
            } else {
                color = getColor(isSelected ? key_chat_inBubbleSelected : key_chat_inBubble);
                gradientColor = null;
            }
            if (gradientColor != null) {
                color = getColor(key_chat_outBubble);
            }
            if (gradientColor == null) {
                gradientColor = 0;
            }
            if (gradientColor != 0 && (gradientShader == null || backgroundHeight != currentBackgroundHeight || currentColor != color || currentGradientColor != gradientColor)) {
                gradientShader = new LinearGradient(0, 0, 0, backgroundHeight, new int[]{gradientColor, color}, null, Shader.TileMode.CLAMP);
                paint.setShader(gradientShader);
                currentColor = color;
                currentGradientColor = gradientColor;
                paint.setColor(0xffffffff);
            } else if (gradientColor == 0) {
                if (gradientShader != null) {
                    gradientShader = null;
                    paint.setShader(null);
                }
                paint.setColor(color);
            }
            currentBackgroundHeight = backgroundHeight;

            topY = top;
            isTopNear = topNear;
            isBottomNear = bottomNear;
        }

        private int dp(float value) {
            if (currentType == TYPE_PREVIEW) {
                return (int) Math.ceil(3 * value);
            } else {
                return AndroidUtilities.dp(value);
            }
        }

        public Paint getPaint() {
            return paint;
        }

        public Drawable[] getShadowDrawables() {
            return shadowDrawable;
        }

        public Drawable getBackgroundDrawable() {
            int newRad = AndroidUtilities.dp(SharedConfig.bubbleRadius);
            int idx;
            if (isTopNear && isBottomNear) {
                idx = 3;
            } else if (isTopNear) {
                idx = 2;
            } else if (isBottomNear) {
                idx = 1;
            } else {
                idx = 0;
            }
            int idx2 = isSelected ? 1 : 0;
            boolean forceSetColor = false;

            boolean drawWithShadow = gradientShader == null && !isSelected;
            int shadowColor = getColor(isOut ? key_chat_outBubbleShadow : key_chat_inBubbleShadow);
            if (currentBackgroundDrawableRadius[idx2][idx] != newRad || (drawWithShadow && shadowDrawableColor[idx] != shadowColor)) {
                currentBackgroundDrawableRadius[idx2][idx] = newRad;
                try {
                    Bitmap bitmap = Bitmap.createBitmap(dp(50), dp(40), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    backupRect.set(getBounds());

                    if (drawWithShadow) {
                        shadowDrawableColor[idx] = shadowColor;

                        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

                        LinearGradient gradientShader = new LinearGradient(0, 0, 0, dp(40), new int[]{0x155F6569, 0x295F6569}, null, Shader.TileMode.CLAMP);
                        shadowPaint.setShader(gradientShader);
                        shadowPaint.setColorFilter(new PorterDuffColorFilter(shadowColor, PorterDuff.Mode.MULTIPLY));

                        shadowPaint.setShadowLayer(2, 0, 1, 0xffffffff);
                        if (AndroidUtilities.density > 1) {
                            setBounds(-1, -1, bitmap.getWidth() + 1, bitmap.getHeight() + 1);
                        } else {
                            setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        }
                        draw(canvas, shadowPaint);

                        if (AndroidUtilities.density > 1) {
                            shadowPaint.setColor(0);
                            shadowPaint.setShadowLayer(0, 0, 0, 0);
                            shadowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                            setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                            draw(canvas, shadowPaint);
                        }
                    }

                    Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    shadowPaint.setColor(0xffffffff);
                    setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    draw(canvas, shadowPaint);

                    backgroundDrawable[idx2][idx] = new NinePatchDrawable(bitmap, getByteBuffer(bitmap.getWidth() / 2 - 1, bitmap.getWidth() / 2 + 1, bitmap.getHeight() / 2 - 1, bitmap.getHeight() / 2 + 1).array(), new Rect(), null);
                    forceSetColor = true;
                    setBounds(backupRect);
                } catch (Throwable ignore) {

                }
            }
            int color;
            if (isSelected) {
                color = getColor(isOut ? key_chat_outBubbleSelected : key_chat_inBubbleSelected);
            } else {
                color = getColor(isOut ? key_chat_outBubble : key_chat_inBubble);
            }
            if (backgroundDrawable[idx2][idx] != null && (backgroundDrawableColor[idx2][idx] != color || forceSetColor)) {
                backgroundDrawable[idx2][idx].setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                backgroundDrawableColor[idx2][idx] = color;
            }
            return backgroundDrawable[idx2][idx];
        }

        public Drawable getShadowDrawable() {
            if (gradientShader == null && !isSelected) {
                return null;
            }
            int newRad = AndroidUtilities.dp(SharedConfig.bubbleRadius);
            int idx;
            if (isTopNear && isBottomNear) {
                idx = 3;
            } else if (isTopNear) {
                idx = 2;
            } else if (isBottomNear) {
                idx = 1;
            } else {
                idx = 0;
            }
            boolean forceSetColor = false;
            if (currentShadowDrawableRadius[idx] != newRad) {
                currentShadowDrawableRadius[idx] = newRad;
                try {
                    Bitmap bitmap = Bitmap.createBitmap(dp(50), dp(40), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

                    LinearGradient gradientShader = new LinearGradient(0, 0, 0, dp(40), new int[]{0x155F6569, 0x295F6569}, null, Shader.TileMode.CLAMP);
                    shadowPaint.setShader(gradientShader);

                    shadowPaint.setShadowLayer(2, 0, 1, 0xffffffff);
                    if (AndroidUtilities.density > 1) {
                        setBounds(-1, -1, bitmap.getWidth() + 1, bitmap.getHeight() + 1);
                    } else {
                        setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    }
                    draw(canvas, shadowPaint);

                    if (AndroidUtilities.density > 1) {
                        shadowPaint.setColor(0);
                        shadowPaint.setShadowLayer(0, 0, 0, 0);
                        shadowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                        setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        draw(canvas, shadowPaint);
                    }

                    shadowDrawable[idx] = new NinePatchDrawable(bitmap, getByteBuffer(bitmap.getWidth() / 2 - 1, bitmap.getWidth() / 2 + 1, bitmap.getHeight() / 2 - 1, bitmap.getHeight() / 2 + 1).array(), new Rect(), null);
                    forceSetColor = true;
                } catch (Throwable ignore) {

                }
            }
            int color = getColor(isOut ? key_chat_outBubbleShadow : key_chat_inBubbleShadow);
            if (shadowDrawable[idx] != null && (shadowDrawableColor[idx] != color || forceSetColor)) {
                shadowDrawable[idx].setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                shadowDrawableColor[idx] = color;
            }
            return shadowDrawable[idx];
        }

        private static ByteBuffer getByteBuffer(int x1, int x2, int y1, int y2) {
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4 * 7 + 4 * 2 + 4 * 2 + 4 * 9).order(ByteOrder.nativeOrder());
            buffer.put((byte) 0x01);
            buffer.put((byte) 2);
            buffer.put((byte) 2);
            buffer.put((byte) 0x09);

            buffer.putInt(0);
            buffer.putInt(0);

            buffer.putInt(0);
            buffer.putInt(0);
            buffer.putInt(0);
            buffer.putInt(0);

            buffer.putInt(0);

            buffer.putInt(x1);
            buffer.putInt(x2);

            buffer.putInt(y1);
            buffer.putInt(y2);

            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);
            buffer.putInt(0x00000001);

            return buffer;
        }

        @Override
        public void draw(Canvas canvas) {
            draw(canvas, null);
        }

        public void draw(Canvas canvas, Paint paintToUse) {
            Rect bounds = getBounds();
            if (paintToUse == null && gradientShader == null) {
                Drawable background = getBackgroundDrawable();
                if (background != null) {
                    background.setBounds(bounds);
                    background.draw(canvas);
                    return;
                }
            }
            int padding = dp(2);
            int rad;
            int nearRad;
            if (currentType == TYPE_PREVIEW) {
                rad = dp(6);
                nearRad = dp(6);
            } else {
                rad = dp(SharedConfig.bubbleRadius);
                nearRad = dp(Math.min(5, SharedConfig.bubbleRadius));
            }
            int smallRad = dp(6);

            Paint p = paintToUse == null ? paint : paintToUse;

            if (paintToUse == null && gradientShader != null) {
                matrix.reset();
                matrix.postTranslate(0, -topY);
                gradientShader.setLocalMatrix(matrix);
            }

            int top = Math.max(bounds.top, 0);
            path.reset();
            if (isOut) {
                if (currentType == TYPE_PREVIEW || paintToUse != null || topY + bounds.bottom - rad < currentBackgroundHeight) {
                    if (currentType == TYPE_MEDIA) {
                        path.moveTo(bounds.right - dp(8) - rad, bounds.bottom - padding);
                    } else {
                        path.moveTo(bounds.right - dp(2.6f), bounds.bottom - padding);
                    }
                    path.lineTo(bounds.left + padding + rad, bounds.bottom - padding);
                    rect.set(bounds.left + padding, bounds.bottom - padding - rad * 2, bounds.left + padding + rad * 2, bounds.bottom - padding);
                    path.arcTo(rect, 90, 90, false);
                } else {
                    path.moveTo(bounds.right - dp(8), top - topY + currentBackgroundHeight);
                    path.lineTo(bounds.left + padding, top - topY + currentBackgroundHeight);
                }
                if (currentType == TYPE_PREVIEW || paintToUse != null || topY + rad * 2 >= 0) {
                    path.lineTo(bounds.left + padding, bounds.top + padding + rad);
                    rect.set(bounds.left + padding, bounds.top + padding, bounds.left + padding + rad * 2, bounds.top + padding + rad * 2);
                    path.arcTo(rect, 180, 90, false);

                    int radToUse = isTopNear ? nearRad : rad;
                    if (currentType == TYPE_MEDIA) {
                        path.lineTo(bounds.right - padding - radToUse, bounds.top + padding);
                        rect.set(bounds.right - padding - radToUse * 2, bounds.top + padding, bounds.right - padding, bounds.top + padding + radToUse * 2);
                    } else {
                        path.lineTo(bounds.right - dp(8) - radToUse, bounds.top + padding);
                        rect.set(bounds.right - dp(8) - radToUse * 2, bounds.top + padding, bounds.right - dp(8), bounds.top + padding + radToUse * 2);
                    }
                    path.arcTo(rect, 270, 90, false);
                } else {
                    path.lineTo(bounds.left + padding, top - topY - dp(2));
                    if (currentType == TYPE_MEDIA) {
                        path.lineTo(bounds.right - padding, top - topY - dp(2));
                    } else {
                        path.lineTo(bounds.right - dp(8), top - topY - dp(2));
                    }
                }
                if (currentType == TYPE_MEDIA) {
                    if (currentType == TYPE_PREVIEW || paintToUse != null || topY + bounds.bottom - rad < currentBackgroundHeight) {
                        int radToUse = isBottomNear ? nearRad : rad;

                        path.lineTo(bounds.right - padding, bounds.bottom - padding - radToUse);
                        rect.set(bounds.right - padding - radToUse * 2, bounds.bottom - padding - radToUse * 2, bounds.right - padding, bounds.bottom - padding);
                        path.arcTo(rect, 0, 90, false);
                    } else {
                        path.lineTo(bounds.right - padding, top - topY + currentBackgroundHeight);
                    }
                } else {
                    if (currentType == TYPE_PREVIEW || paintToUse != null || topY + bounds.bottom - smallRad * 2 < currentBackgroundHeight) {
                        path.lineTo(bounds.right - dp(8), bounds.bottom - padding - smallRad - dp(3));
                        rect.set(bounds.right - dp(8), bounds.bottom - padding - smallRad * 2 - dp(9), bounds.right - dp(7) + smallRad * 2, bounds.bottom - padding - dp(1));
                        path.arcTo(rect, 180, -83, false);
                    } else {
                        path.lineTo(bounds.right - dp(8), top - topY + currentBackgroundHeight);
                    }
                }
            } else {
                if (currentType == TYPE_PREVIEW || paintToUse != null || topY + bounds.bottom - rad < currentBackgroundHeight) {
                    if (currentType == TYPE_MEDIA) {
                        path.moveTo(bounds.left + dp(8) + rad, bounds.bottom - padding);
                    } else {
                        path.moveTo(bounds.left + dp(2.6f), bounds.bottom - padding);
                    }
                    path.lineTo(bounds.right - padding - rad, bounds.bottom - padding);
                    rect.set(bounds.right - padding - rad * 2, bounds.bottom - padding - rad * 2, bounds.right - padding, bounds.bottom - padding);
                    path.arcTo(rect, 90, -90, false);
                } else {
                    path.moveTo(bounds.left + dp(8), top - topY + currentBackgroundHeight);
                    path.lineTo(bounds.right - padding, top - topY + currentBackgroundHeight);
                }
                if (currentType == TYPE_PREVIEW || paintToUse != null || topY + rad * 2 >= 0) {
                    path.lineTo(bounds.right - padding, bounds.top + padding + rad);
                    rect.set(bounds.right - padding - rad * 2, bounds.top + padding, bounds.right - padding, bounds.top + padding + rad * 2);
                    path.arcTo(rect, 0, -90, false);

                    int radToUse = isTopNear ? nearRad : rad;
                    if (currentType == TYPE_MEDIA) {
                        path.lineTo(bounds.left + padding + radToUse, bounds.top + padding);
                        rect.set(bounds.left + padding, bounds.top + padding, bounds.left + padding + radToUse * 2, bounds.top + padding + radToUse * 2);
                    } else {
                        path.lineTo(bounds.left + dp(8) + radToUse, bounds.top + padding);
                        rect.set(bounds.left + dp(8), bounds.top + padding, bounds.left + dp(8) + radToUse * 2, bounds.top + padding + radToUse * 2);
                    }
                    path.arcTo(rect, 270, -90, false);
                } else {
                    path.lineTo(bounds.right - padding, top - topY - dp(2));
                    if (currentType == TYPE_MEDIA) {
                        path.lineTo(bounds.left + padding, top - topY - dp(2));
                    } else {
                        path.lineTo(bounds.left + dp(8), top - topY - dp(2));
                    }
                }
                if (currentType == TYPE_MEDIA) {
                    if (currentType == TYPE_PREVIEW || paintToUse != null || topY + bounds.bottom - rad < currentBackgroundHeight) {
                        int radToUse = isBottomNear ? nearRad : rad;

                        path.lineTo(bounds.left + padding, bounds.bottom - padding - radToUse);
                        rect.set(bounds.left + padding, bounds.bottom - padding - radToUse * 2, bounds.left + padding + radToUse * 2, bounds.bottom - padding);
                        path.arcTo(rect, 180, -90, false);
                    } else {
                        path.lineTo(bounds.left + padding, top - topY + currentBackgroundHeight);
                    }
                } else {
                    if (currentType == TYPE_PREVIEW || paintToUse != null || topY + bounds.bottom - smallRad * 2 < currentBackgroundHeight) {
                        path.lineTo(bounds.left + dp(8), bounds.bottom - padding - smallRad - dp(3));
                        rect.set(bounds.left + dp(7) - smallRad * 2, bounds.bottom - padding - smallRad * 2 - dp(9), bounds.left + dp(8), bounds.bottom - padding - dp(1));
                        path.arcTo(rect, 0, 83, false);
                    } else {
                        path.lineTo(bounds.left + dp(8), top - topY + currentBackgroundHeight);
                    }
                }
            }
            path.close();

            canvas.drawPath(path, p);
            if (gradientShader != null && isSelected) {
                selectedPaint.setColor(getColor(key_chat_outBubbleGradientSelectedOverlay));
                canvas.drawPath(path, selectedPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            if (this.alpha != alpha) {
                this.alpha = alpha;
                paint.setAlpha(alpha);
                if (isOut) {
                    selectedPaint.setAlpha((int) (Color.alpha(getColor(key_chat_outBubbleGradientSelectedOverlay)) * (alpha / 255.0f)));
                }
            }
            if (gradientShader == null) {
                Drawable background = getBackgroundDrawable();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (background.getAlpha() != alpha) {
                        background.setAlpha(alpha);
                    }
                } else {
                    background.setAlpha(alpha);
                }
            }
        }

        @Override
        public void setColorFilter(int color, PorterDuff.Mode mode) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }
    }


    private static final Object sync = new Object();
    private static final Object wallpaperSync = new Object();

    public static final int ACTION_BAR_PHOTO_VIEWER_COLOR = 0x7f000000;
    public static final int ACTION_BAR_MEDIA_PICKER_COLOR = 0xff333333;
    public static final int ACTION_BAR_VIDEO_EDIT_COLOR = 0xff000000;
    public static final int ACTION_BAR_PLAYER_COLOR = 0xffffffff;
    public static final int ACTION_BAR_PICKER_SELECTOR_COLOR = 0xff3d3d3d;
    public static final int ACTION_BAR_WHITE_SELECTOR_COLOR = 0x40ffffff;
    public static final int ACTION_BAR_AUDIO_SELECTOR_COLOR = 0x2f000000;
    public static final int ARTICLE_VIEWER_MEDIA_PROGRESS_COLOR = 0xffffffff;

    public static final int AUTO_NIGHT_TYPE_NONE = 0;
    public static final int AUTO_NIGHT_TYPE_SCHEDULED = 1;
    public static final int AUTO_NIGHT_TYPE_AUTOMATIC = 2;
    public static final int AUTO_NIGHT_TYPE_SYSTEM = 3;

    private static final int LIGHT_SENSOR_THEME_SWITCH_DELAY = 1800;
    private static final int LIGHT_SENSOR_THEME_SWITCH_NEAR_DELAY = 12000;
    private static final int LIGHT_SENSOR_THEME_SWITCH_NEAR_THRESHOLD = 12000;
    private static SensorManager sensorManager;
    private static Sensor lightSensor;
    private static boolean lightSensorRegistered;
    private static float lastBrightnessValue = 1.0f;
    private static long lastThemeSwitchTime;
    private static boolean switchDayRunnableScheduled;
    private static boolean switchNightRunnableScheduled;

    public static int DEFALT_THEME_ACCENT_ID = 99;
    public static int selectedAutoNightType = AUTO_NIGHT_TYPE_NONE;
    public static boolean autoNightScheduleByLocation;
    public static float autoNightBrighnessThreshold = 0.25f;
    public static int autoNightDayStartTime = 22 * 60;
    public static int autoNightDayEndTime = 8 * 60;
    public static int autoNightSunsetTime = 22 * 60;
    public static int autoNightLastSunCheckDay = -1;
    public static int autoNightSunriseTime = 8 * 60;
    public static String autoNightCityName = "";
    public static double autoNightLocationLatitude = 10000;
    public static double autoNightLocationLongitude = 10000;

    private static Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static int loadingCurrentTheme;
    private static int lastLoadingCurrentThemeTime;

    public static PorterDuffColorFilter colorFilter;
    public static PorterDuffColorFilter colorPressedFilter;
    public static PorterDuffColorFilter colorFilter2;
    public static PorterDuffColorFilter colorPressedFilter2;
    private static boolean isCustomTheme;
    private static int serviceMessageColor;
    private static int serviceSelectedMessageColor;
    public static int serviceMessageColorBackup;
    public static int serviceSelectedMessageColorBackup;
    private static int serviceMessage2Color;
    private static int serviceSelectedMessage2Color;
    public static int currentColor;
    private static int currentSelectedColor;
    private static Drawable wallpaper;
    private static Drawable themedWallpaper;
    private static int themedWallpaperFileOffset;
    private static String themedWallpaperLink;
    private static boolean isWallpaperMotion;
    private static boolean isPatternWallpaper;

    public static Paint dividerPaint;
    public static Paint linkSelectionPaint;
    public static Paint checkboxSquare_eraserPaint;
    public static Paint checkboxSquare_checkPaint;
    public static Paint checkboxSquare_backgroundPaint;
    public static Paint avatar_backgroundPaint;

    public static Drawable listSelector;
    public static Drawable[] avatarDrawables = new Drawable[12];

    public static Drawable moveUpDrawable;

    public static Paint dialogs_onlineCirclePaint;
    public static Paint dialogs_tabletSeletedPaint;
    public static Paint dialogs_pinnedPaint;
    public static Paint dialogs_countPaint;
    public static Paint dialogs_errorPaint;
    public static Paint dialogs_countGrayPaint;
    public static TextPaint[] dialogs_namePaint;
    public static TextPaint[] dialogs_nameEncryptedPaint;
    public static TextPaint dialogs_searchNamePaint;
    public static TextPaint dialogs_searchNameEncryptedPaint;
    public static TextPaint[] dialogs_messagePaint;
    public static TextPaint dialogs_messageNamePaint;
    public static TextPaint[] dialogs_messagePrintingPaint;
    public static TextPaint dialogs_timePaint;
    public static TextPaint dialogs_countTextPaint;
    public static TextPaint dialogs_archiveTextPaint;
    public static TextPaint dialogs_onlinePaint;
    public static TextPaint dialogs_offlinePaint;
    public static Drawable dialogs_checkDrawable;
    public static Drawable dialogs_playDrawable;
    public static Drawable dialogs_checkReadDrawable;
    public static Drawable dialogs_halfCheckDrawable;
    public static Drawable dialogs_clockDrawable;
    public static Drawable dialogs_errorDrawable;
    public static Drawable dialogs_reorderDrawable;
    public static Drawable dialogs_lockDrawable;
    public static Drawable dialogs_groupDrawable;
    public static Drawable dialogs_broadcastDrawable;
    public static Drawable dialogs_botDrawable;
    public static Drawable dialogs_muteDrawable;
    public static Drawable dialogs_verifiedDrawable;

    public static Drawable dialogs_verifiedCheckDrawable;
    public static Drawable dialogs_pinnedDrawable;
    public static Drawable dialogs_mentionDrawable;
    public static Drawable dialogs_holidayDrawable;
    public static boolean dialogs_archiveDrawableRecolored;
    public static boolean dialogs_hidePsaDrawableRecolored;
    public static boolean dialogs_archiveAvatarDrawableRecolored;
    private static int dialogs_holidayDrawableOffsetX;
    private static int dialogs_holidayDrawableOffsetY;
    private static long lastHolidayCheckTime;
    private static boolean canStartHolidayAnimation;

    public static TextPaint profile_aboutTextPaint;
    public static Drawable profile_verifiedDrawable;
    public static Drawable profile_verifiedCheckDrawable;

    public static Paint chat_docBackPaint;
    public static Paint chat_deleteProgressPaint;
    public static Paint chat_botProgressPaint;
    public static Paint chat_urlPaint;
    public static Paint chat_textSearchSelectionPaint;
    public static Paint chat_instantViewRectPaint;
    public static Paint chat_pollTimerPaint;
    public static Paint chat_replyLinePaint;
    public static Paint chat_msgErrorPaint;
    public static Paint chat_statusPaint;
    public static Paint chat_statusRecordPaint;
    public static Paint chat_actionBackgroundPaint;
    public static Paint chat_timeBackgroundPaint;
    public static Paint chat_composeBackgroundPaint;
    public static Paint chat_radialProgressPaint;
    public static Paint chat_radialProgress2Paint;
    public static TextPaint chat_msgTextPaint;
    public static TextPaint chat_actionTextPaint;
    public static TextPaint chat_msgBotButtonPaint;
    public static TextPaint chat_msgGameTextPaint;
    public static TextPaint chat_msgTextPaintOneEmoji;
    public static TextPaint chat_msgTextPaintTwoEmoji;
    public static TextPaint chat_msgTextPaintThreeEmoji;
    public static TextPaint chat_infoPaint;
    public static TextPaint chat_stickerCommentCountPaint;
    public static TextPaint chat_livePaint;
    public static TextPaint chat_docNamePaint;
    public static TextPaint chat_locationTitlePaint;
    public static TextPaint chat_locationAddressPaint;
    public static TextPaint chat_durationPaint;
    public static TextPaint chat_gamePaint;
    public static TextPaint chat_shipmentPaint;
    public static TextPaint chat_instantViewPaint;
    public static TextPaint chat_audioTimePaint;
    public static TextPaint chat_audioTitlePaint;
    public static TextPaint chat_audioPerformerPaint;
    public static TextPaint chat_botButtonPaint;
    public static TextPaint chat_contactNamePaint;
    public static TextPaint chat_contactPhonePaint;
    public static TextPaint chat_timePaint;
    public static TextPaint chat_adminPaint;
    public static TextPaint chat_namePaint;
    public static TextPaint chat_forwardNamePaint;
    public static TextPaint chat_replyNamePaint;
    public static TextPaint chat_replyTextPaint;
    public static TextPaint chat_contextResult_titleTextPaint;
    public static TextPaint chat_contextResult_descriptionTextPaint;

    public static Drawable chat_msgNoSoundDrawable;
    public static Drawable chat_composeShadowDrawable;
    public static Drawable chat_roundVideoShadow;
    public static MessageDrawable chat_msgInDrawable;
    public static MessageDrawable chat_msgInSelectedDrawable;
    public static MessageDrawable chat_msgOutDrawable;
    public static MessageDrawable chat_msgOutSelectedDrawable;
    public static MessageDrawable chat_msgInMediaDrawable;
    public static MessageDrawable chat_msgInMediaSelectedDrawable;
    public static MessageDrawable chat_msgOutMediaDrawable;
    public static MessageDrawable chat_msgOutMediaSelectedDrawable;

    public static Drawable chat_msgOutCheckDrawable;
    public static Drawable chat_msgOutCheckSelectedDrawable;
    public static Drawable chat_msgOutCheckReadDrawable;
    public static Drawable chat_msgOutCheckReadSelectedDrawable;
    public static Drawable chat_msgOutHalfCheckDrawable;
    public static Drawable chat_msgOutHalfCheckSelectedDrawable;
    public static Drawable chat_msgOutClockDrawable;
    public static Drawable chat_msgOutSelectedClockDrawable;
    public static Drawable chat_msgInClockDrawable;
    public static Drawable chat_msgInSelectedClockDrawable;
    public static Drawable chat_msgMediaCheckDrawable;
    public static Drawable chat_msgMediaHalfCheckDrawable;
    public static Drawable chat_msgMediaClockDrawable;
    public static Drawable chat_msgStickerCheckDrawable;
    public static Drawable chat_msgStickerHalfCheckDrawable;
    public static Drawable chat_msgStickerClockDrawable;
    public static Drawable chat_msgStickerViewsDrawable;
    public static Drawable chat_msgStickerRepliesDrawable;
    public static Drawable chat_msgInViewsDrawable;
    public static Drawable chat_msgInViewsSelectedDrawable;
    public static Drawable chat_msgOutViewsDrawable;
    public static Drawable chat_msgOutViewsSelectedDrawable;
    public static Drawable chat_msgInRepliesDrawable;
    public static Drawable chat_msgInRepliesSelectedDrawable;
    public static Drawable chat_msgOutRepliesDrawable;
    public static Drawable chat_msgOutRepliesSelectedDrawable;
    public static Drawable chat_msgMediaViewsDrawable;
    public static Drawable chat_msgMediaRepliesDrawable;
    public static Drawable chat_msgInMenuDrawable;
    public static Drawable chat_msgInMenuSelectedDrawable;
    public static Drawable chat_msgOutMenuDrawable;
    public static Drawable chat_msgOutMenuSelectedDrawable;
    public static Drawable chat_msgMediaMenuDrawable;
    public static Drawable chat_msgInInstantDrawable;
    public static Drawable chat_msgOutInstantDrawable;
    public static Drawable chat_msgErrorDrawable;
    public static Drawable chat_muteIconDrawable;
    public static Drawable chat_lockIconDrawable;
    public static Drawable chat_inlineResultFile;
    public static Drawable chat_inlineResultAudio;
    public static Drawable chat_inlineResultLocation;
    public static Drawable chat_redLocationIcon;
    public static Drawable chat_msgOutBroadcastDrawable;
    public static Drawable chat_msgMediaBroadcastDrawable;
    public static Drawable chat_msgOutLocationDrawable;
    public static Drawable chat_msgBroadcastDrawable;
    public static Drawable chat_msgBroadcastMediaDrawable;
    public static Drawable chat_contextResult_shadowUnderSwitchDrawable;
    public static Drawable chat_shareDrawable;
    public static Drawable chat_shareIconDrawable;
    public static Drawable chat_replyIconDrawable;
    public static Drawable chat_goIconDrawable;
    public static Drawable chat_botLinkDrawalbe;
    public static Drawable chat_botInlineDrawable;
    public static Drawable chat_systemDrawable;
    public static Drawable chat_commentDrawable;
    public static Drawable chat_commentStickerDrawable;
    public static Drawable chat_commentArrowDrawable;
    public static Drawable[] chat_msgInCallDrawable = new Drawable[2];
    public static Drawable[] chat_msgInCallSelectedDrawable = new Drawable[2];
    public static Drawable[] chat_msgOutCallDrawable = new Drawable[2];
    public static Drawable[] chat_msgOutCallSelectedDrawable = new Drawable[2];
    public static Drawable[] chat_pollCheckDrawable = new Drawable[2];
    public static Drawable[] chat_pollCrossDrawable = new Drawable[2];
    public static Drawable[] chat_pollHintDrawable = new Drawable[2];
    public static Drawable[] chat_psaHelpDrawable = new Drawable[2];

    public static Drawable chat_msgCallUpGreenDrawable;
    public static Drawable chat_msgCallDownRedDrawable;
    public static Drawable chat_msgCallDownGreenDrawable;

    public static Drawable chat_msgAvatarLiveLocationDrawable;
    public static Drawable chat_attachEmptyDrawable;

    public static Drawable[] chat_locationDrawable = new Drawable[2];
    public static Drawable[] chat_contactDrawable = new Drawable[2];
    public static Drawable[] chat_cornerOuter = new Drawable[4];
    public static Drawable[] chat_cornerInner = new Drawable[4];
    public static Drawable[][] chat_fileStatesDrawable = new Drawable[10][2];

    public static Drawable[][] chat_photoStatesDrawables = new Drawable[13][2];

    public static Drawable calllog_msgCallUpRedDrawable;
    public static Drawable calllog_msgCallUpGreenDrawable;
    public static Drawable calllog_msgCallDownRedDrawable;
    public static Drawable calllog_msgCallDownGreenDrawable;

    public static Path[] chat_filePath = new Path[2];
    public static Drawable chat_flameIcon;
    public static Drawable chat_gifIcon;



    public static final String key_dialogBackground = "dialogBackground";
    public static final String key_dialogBackgroundGray = "dialogBackgroundGray";
    public static final String key_dialogTextBlack = "dialogTextBlack";
    public static final String key_dialogTextLink = "dialogTextLink";
    public static final String key_dialogLinkSelection = "dialogLinkSelection";
    public static final String key_dialogTextRed = "dialogTextRed";
    public static final String key_dialogTextRed2 = "dialogTextRed2";
    public static final String key_dialogTextBlue = "dialogTextBlue";
    public static final String key_dialogTextBlue2 = "dialogTextBlue2";
    public static final String key_dialogTextBlue3 = "dialogTextBlue3";
    public static final String key_dialogTextBlue4 = "dialogTextBlue4";
    public static final String key_dialogTextGray = "dialogTextGray";
    public static final String key_dialogTextGray2 = "dialogTextGray2";
    public static final String key_dialogTextGray3 = "dialogTextGray3";
    public static final String key_dialogTextGray4 = "dialogTextGray4";
    public static final String key_dialogTextHint = "dialogTextHint";
    public static final String key_dialogInputField = "dialogInputField";
    public static final String key_dialogInputFieldActivated = "dialogInputFieldActivated";
    public static final String key_dialogCheckboxSquareBackground = "dialogCheckboxSquareBackground";
    public static final String key_dialogCheckboxSquareCheck = "dialogCheckboxSquareCheck";
    public static final String key_dialogCheckboxSquareUnchecked = "dialogCheckboxSquareUnchecked";
    public static final String key_dialogCheckboxSquareDisabled = "dialogCheckboxSquareDisabled";
    public static final String key_dialogScrollGlow = "dialogScrollGlow";
    public static final String key_dialogRoundCheckBox = "dialogRoundCheckBox";
    public static final String key_dialogRoundCheckBoxCheck = "dialogRoundCheckBoxCheck";
    public static final String key_dialogBadgeBackground = "dialogBadgeBackground";
    public static final String key_dialogBadgeText = "dialogBadgeText";
    public static final String key_dialogRadioBackground = "dialogRadioBackground";
    public static final String key_dialogRadioBackgroundChecked = "dialogRadioBackgroundChecked";
    public static final String key_dialogProgressCircle = "dialogProgressCircle";
    public static final String key_dialogLineProgress = "dialogLineProgress";
    public static final String key_dialogLineProgressBackground = "dialogLineProgressBackground";
    public static final String key_dialogButton = "dialogButton";
    public static final String key_dialogButtonSelector = "dialogButtonSelector";
    public static final String key_dialogIcon = "dialogIcon";
    public static final String key_dialogRedIcon = "dialogRedIcon";
    public static final String key_dialogGrayLine = "dialogGrayLine";
    public static final String key_dialogTopBackground = "dialogTopBackground";
    public static final String key_dialogCameraIcon = "dialogCameraIcon";
    public static final String key_dialog_inlineProgressBackground = "dialog_inlineProgressBackground";
    public static final String key_dialog_inlineProgress = "dialog_inlineProgress";
    public static final String key_dialogSearchBackground = "dialogSearchBackground";
    public static final String key_dialogSearchHint = "dialogSearchHint";
    public static final String key_dialogSearchIcon = "dialogSearchIcon";
    public static final String key_dialogSearchText = "dialogSearchText";
    public static final String key_dialogFloatingButton = "dialogFloatingButton";
    public static final String key_dialogFloatingButtonPressed = "dialogFloatingButtonPressed";
    public static final String key_dialogFloatingIcon = "dialogFloatingIcon";
    public static final String key_dialogShadowLine = "dialogShadowLine";
    public static final String key_dialogEmptyImage = "dialogEmptyImage";
    public static final String key_dialogEmptyText = "dialogEmptyText";

    public static final String key_windowBackgroundWhite = "windowBackgroundWhite";
    public static final String key_windowBackgroundUnchecked = "windowBackgroundUnchecked";
    public static final String key_windowBackgroundChecked = "windowBackgroundChecked";
    public static final String key_windowBackgroundCheckText = "windowBackgroundCheckText";
    public static final String key_progressCircle = "progressCircle";
    public static final String key_listSelector = "listSelectorSDK21";
    public static final String key_windowBackgroundWhiteInputField = "windowBackgroundWhiteInputField";
    public static final String key_windowBackgroundWhiteInputFieldActivated = "windowBackgroundWhiteInputFieldActivated";
    public static final String key_windowBackgroundWhiteGrayIcon = "windowBackgroundWhiteGrayIcon";
    public static final String key_windowBackgroundWhiteBlueText = "windowBackgroundWhiteBlueText";
    public static final String key_windowBackgroundWhiteBlueText2 = "windowBackgroundWhiteBlueText2";
    public static final String key_windowBackgroundWhiteBlueText3 = "windowBackgroundWhiteBlueText3";
    public static final String key_windowBackgroundWhiteBlueText4 = "windowBackgroundWhiteBlueText4";
    public static final String key_windowBackgroundWhiteBlueText5 = "windowBackgroundWhiteBlueText5";
    public static final String key_windowBackgroundWhiteBlueText6 = "windowBackgroundWhiteBlueText6";
    public static final String key_windowBackgroundWhiteBlueText7 = "windowBackgroundWhiteBlueText7";
    public static final String key_windowBackgroundWhiteBlueButton = "windowBackgroundWhiteBlueButton";
    public static final String key_windowBackgroundWhiteBlueIcon = "windowBackgroundWhiteBlueIcon";
    public static final String key_windowBackgroundWhiteGreenText = "windowBackgroundWhiteGreenText";
    public static final String key_windowBackgroundWhiteGreenText2 = "windowBackgroundWhiteGreenText2";
    public static final String key_windowBackgroundWhiteRedText = "windowBackgroundWhiteRedText";
    public static final String key_windowBackgroundWhiteRedText2 = "windowBackgroundWhiteRedText2";
    public static final String key_windowBackgroundWhiteRedText3 = "windowBackgroundWhiteRedText3";
    public static final String key_windowBackgroundWhiteRedText4 = "windowBackgroundWhiteRedText4";
    public static final String key_windowBackgroundWhiteRedText5 = "windowBackgroundWhiteRedText5";
    public static final String key_windowBackgroundWhiteRedText6 = "windowBackgroundWhiteRedText6";
    public static final String key_windowBackgroundWhiteGrayText = "windowBackgroundWhiteGrayText";
    public static final String key_windowBackgroundWhiteGrayText2 = "windowBackgroundWhiteGrayText2";
    public static final String key_windowBackgroundWhiteGrayText3 = "windowBackgroundWhiteGrayText3";
    public static final String key_windowBackgroundWhiteGrayText4 = "windowBackgroundWhiteGrayText4";
    public static final String key_windowBackgroundWhiteGrayText5 = "windowBackgroundWhiteGrayText5";
    public static final String key_windowBackgroundWhiteGrayText6 = "windowBackgroundWhiteGrayText6";
    public static final String key_windowBackgroundWhiteGrayText7 = "windowBackgroundWhiteGrayText7";
    public static final String key_windowBackgroundWhiteGrayText8 = "windowBackgroundWhiteGrayText8";
    public static final String key_windowBackgroundWhiteGrayLine = "windowBackgroundWhiteGrayLine";
    public static final String key_windowBackgroundWhiteBlackText = "windowBackgroundWhiteBlackText";
    public static final String key_windowBackgroundWhiteHintText = "windowBackgroundWhiteHintText";
    public static final String key_windowBackgroundWhiteValueText = "windowBackgroundWhiteValueText";
    public static final String key_windowBackgroundWhiteLinkText = "windowBackgroundWhiteLinkText";
    public static final String key_windowBackgroundWhiteLinkSelection = "windowBackgroundWhiteLinkSelection";
    public static final String key_windowBackgroundWhiteBlueHeader = "windowBackgroundWhiteBlueHeader";
    public static final String key_switchTrack = "switchTrack";
    public static final String key_switchTrackChecked = "switchTrackChecked";
    public static final String key_switchTrackBlue = "switchTrackBlue";
    public static final String key_switchTrackBlueChecked = "switchTrackBlueChecked";
    public static final String key_switchTrackBlueThumb = "switchTrackBlueThumb";
    public static final String key_switchTrackBlueThumbChecked = "switchTrackBlueThumbChecked";
    public static final String key_switchTrackBlueSelector = "switchTrackBlueSelector";
    public static final String key_switchTrackBlueSelectorChecked = "switchTrackBlueSelectorChecked";
    public static final String key_switch2Track = "switch2Track";
    public static final String key_switch2TrackChecked = "switch2TrackChecked";
    public static final String key_checkboxSquareBackground = "checkboxSquareBackground";
    public static final String key_checkboxSquareCheck = "checkboxSquareCheck";
    public static final String key_checkboxSquareUnchecked = "checkboxSquareUnchecked";
    public static final String key_checkboxSquareDisabled = "checkboxSquareDisabled";
    public static final String key_windowBackgroundGray = "windowBackgroundGray";
    public static final String key_windowBackgroundGrayShadow = "windowBackgroundGrayShadow";
    public static final String key_emptyListPlaceholder = "emptyListPlaceholder";
    public static final String key_divider = "divider";
    public static final String key_graySection = "graySection";
    public static final String key_graySectionText = "key_graySectionText";
    public static final String key_radioBackground = "radioBackground";
    public static final String key_radioBackgroundChecked = "radioBackgroundChecked";
    public static final String key_checkbox = "checkbox";
    public static final String key_checkboxDisabled = "checkboxDisabled";
    public static final String key_checkboxCheck = "checkboxCheck";
    public static final String key_fastScrollActive = "fastScrollActive";
    public static final String key_fastScrollInactive = "fastScrollInactive";
    public static final String key_fastScrollText = "fastScrollText";

    public static final String key_inappPlayerPerformer = "inappPlayerPerformer";
    public static final String key_inappPlayerTitle = "inappPlayerTitle";
    public static final String key_inappPlayerBackground = "inappPlayerBackground";
    public static final String key_inappPlayerPlayPause = "inappPlayerPlayPause";
    public static final String key_inappPlayerClose = "inappPlayerClose";

    public static final String key_returnToCallBackground = "returnToCallBackground";
    public static final String key_returnToCallText = "returnToCallText";

    public static final String key_contextProgressInner1 = "contextProgressInner1";
    public static final String key_contextProgressOuter1 = "contextProgressOuter1";
    public static final String key_contextProgressInner2 = "contextProgressInner2";
    public static final String key_contextProgressOuter2 = "contextProgressOuter2";
    public static final String key_contextProgressInner3 = "contextProgressInner3";
    public static final String key_contextProgressOuter3 = "contextProgressOuter3";
    public static final String key_contextProgressInner4 = "contextProgressInner4";
    public static final String key_contextProgressOuter4 = "contextProgressOuter4";

    public static final String key_avatar_text = "avatar_text";
    public static final String key_avatar_backgroundSaved = "avatar_backgroundSaved";
    public static final String key_avatar_backgroundArchived = "avatar_backgroundArchived";
    public static final String key_avatar_backgroundArchivedHidden = "avatar_backgroundArchivedHidden";
    public static final String key_avatar_backgroundRed = "avatar_backgroundRed";
    public static final String key_avatar_backgroundOrange = "avatar_backgroundOrange";
    public static final String key_avatar_backgroundViolet = "avatar_backgroundViolet";
    public static final String key_avatar_backgroundGreen = "avatar_backgroundGreen";
    public static final String key_avatar_backgroundCyan = "avatar_backgroundCyan";
    public static final String key_avatar_backgroundBlue = "avatar_backgroundBlue";
    public static final String key_avatar_backgroundPink = "avatar_backgroundPink";

    public static final String key_avatar_backgroundInProfileBlue = "avatar_backgroundInProfileBlue";
    public static final String key_avatar_backgroundActionBarBlue = "avatar_backgroundActionBarBlue";
    public static final String key_avatar_actionBarSelectorBlue = "avatar_actionBarSelectorBlue";
    public static final String key_avatar_actionBarIconBlue = "avatar_actionBarIconBlue";
    public static final String key_avatar_subtitleInProfileBlue = "avatar_subtitleInProfileBlue";

    public static final String key_avatar_nameInMessageRed = "avatar_nameInMessageRed";
    public static final String key_avatar_nameInMessageOrange = "avatar_nameInMessageOrange";
    public static final String key_avatar_nameInMessageViolet = "avatar_nameInMessageViolet";
    public static final String key_avatar_nameInMessageGreen = "avatar_nameInMessageGreen";
    public static final String key_avatar_nameInMessageCyan = "avatar_nameInMessageCyan";
    public static final String key_avatar_nameInMessageBlue = "avatar_nameInMessageBlue";
    public static final String key_avatar_nameInMessagePink = "avatar_nameInMessagePink";

    public static String[] keys_avatar_background = {key_avatar_backgroundRed, key_avatar_backgroundOrange, key_avatar_backgroundViolet, key_avatar_backgroundGreen, key_avatar_backgroundCyan, key_avatar_backgroundBlue, key_avatar_backgroundPink};
    public static String[] keys_avatar_nameInMessage = {key_avatar_nameInMessageRed, key_avatar_nameInMessageOrange, key_avatar_nameInMessageViolet, key_avatar_nameInMessageGreen, key_avatar_nameInMessageCyan, key_avatar_nameInMessageBlue, key_avatar_nameInMessagePink};

    public static final String key_actionBarDefault = "actionBarDefault";
    public static final String key_actionBarDefaultSelector = "actionBarDefaultSelector";
    public static final String key_actionBarWhiteSelector = "actionBarWhiteSelector";
    public static final String key_actionBarDefaultIcon = "actionBarDefaultIcon";
    public static final String key_actionBarTipBackground = "actionBarTipBackground";
    public static final String key_actionBarActionModeDefault = "actionBarActionModeDefault";
    public static final String key_actionBarActionModeDefaultTop = "actionBarActionModeDefaultTop";
    public static final String key_actionBarActionModeDefaultIcon = "actionBarActionModeDefaultIcon";
    public static final String key_actionBarActionModeDefaultSelector = "actionBarActionModeDefaultSelector";
    public static final String key_actionBarDefaultTitle = "actionBarDefaultTitle";
    public static final String key_actionBarDefaultSubtitle = "actionBarDefaultSubtitle";
    public static final String key_actionBarDefaultSearch = "actionBarDefaultSearch";
    public static final String key_actionBarDefaultSearchPlaceholder = "actionBarDefaultSearchPlaceholder";
    public static final String key_actionBarDefaultSubmenuItem = "actionBarDefaultSubmenuItem";
    public static final String key_actionBarDefaultSubmenuItemIcon = "actionBarDefaultSubmenuItemIcon";
    public static final String key_actionBarDefaultSubmenuBackground = "actionBarDefaultSubmenuBackground";
    public static final String key_actionBarTabActiveText = "actionBarTabActiveText";
    public static final String key_actionBarTabUnactiveText = "actionBarTabUnactiveText";
    public static final String key_actionBarTabLine = "actionBarTabLine";
    public static final String key_actionBarTabSelector = "actionBarTabSelector";
    public static final String key_actionBarDefaultArchived = "actionBarDefaultArchived";
    public static final String key_actionBarDefaultArchivedSelector = "actionBarDefaultArchivedSelector";
    public static final String key_actionBarDefaultArchivedIcon = "actionBarDefaultArchivedIcon";
    public static final String key_actionBarDefaultArchivedTitle = "actionBarDefaultArchivedTitle";
    public static final String key_actionBarDefaultArchivedSearch = "actionBarDefaultArchivedSearch";
    public static final String key_actionBarDefaultArchivedSearchPlaceholder = "actionBarDefaultSearchArchivedPlaceholder";

    public static final String key_actionBarBrowser = "actionBarBrowser";

    public static final String key_chats_onlineCircle = "chats_onlineCircle";
    public static final String key_chats_unreadCounter = "chats_unreadCounter";
    public static final String key_chats_unreadCounterMuted = "chats_unreadCounterMuted";
    public static final String key_chats_unreadCounterText = "chats_unreadCounterText";
    public static final String key_chats_name = "chats_name";
    public static final String key_chats_nameArchived = "chats_nameArchived";
    public static final String key_chats_secretName = "chats_secretName";
    public static final String key_chats_secretIcon = "chats_secretIcon";
    public static final String key_chats_nameIcon = "chats_nameIcon";
    public static final String key_chats_pinnedIcon = "chats_pinnedIcon";
    public static final String key_chats_archiveBackground = "chats_archiveBackground";
    public static final String key_chats_archivePinBackground = "chats_archivePinBackground";
    public static final String key_chats_archiveIcon = "chats_archiveIcon";
    public static final String key_chats_archiveText = "chats_archiveText";
    public static final String key_chats_message = "chats_message";
    public static final String key_chats_messageArchived = "chats_messageArchived";
    public static final String key_chats_message_threeLines = "chats_message_threeLines";
    public static final String key_chats_draft = "chats_draft";
    public static final String key_chats_nameMessage = "chats_nameMessage";
    public static final String key_chats_nameMessageArchived = "chats_nameMessageArchived";
    public static final String key_chats_nameMessage_threeLines = "chats_nameMessage_threeLines";
    public static final String key_chats_nameMessageArchived_threeLines = "chats_nameMessageArchived_threeLines";
    public static final String key_chats_attachMessage = "chats_attachMessage";
    public static final String key_chats_actionMessage = "chats_actionMessage";
    public static final String key_chats_date = "chats_date";
    public static final String key_chats_pinnedOverlay = "chats_pinnedOverlay";
    public static final String key_chats_tabletSelectedOverlay = "chats_tabletSelectedOverlay";
    public static final String key_chats_sentCheck = "chats_sentCheck";
    public static final String key_chats_sentReadCheck = "chats_sentReadCheck";
    public static final String key_chats_sentClock = "chats_sentClock";
    public static final String key_chats_sentError = "chats_sentError";
    public static final String key_chats_sentErrorIcon = "chats_sentErrorIcon";
    public static final String key_chats_verifiedBackground = "chats_verifiedBackground";
    public static final String key_chats_verifiedCheck = "chats_verifiedCheck";
    public static final String key_chats_muteIcon = "chats_muteIcon";
    public static final String key_chats_mentionIcon = "chats_mentionIcon";
    public static final String key_chats_menuTopShadow = "chats_menuTopShadow";
    public static final String key_chats_menuTopShadowCats = "chats_menuTopShadowCats";
    public static final String key_chats_menuBackground = "chats_menuBackground";
    public static final String key_chats_menuItemText = "chats_menuItemText";
    public static final String key_chats_menuItemCheck = "chats_menuItemCheck";
    public static final String key_chats_menuItemIcon = "chats_menuItemIcon";
    public static final String key_chats_menuName = "chats_menuName";
    public static final String key_chats_menuPhone = "chats_menuPhone";
    public static final String key_chats_menuPhoneCats = "chats_menuPhoneCats";
    public static final String key_chats_menuTopBackgroundCats = "chats_menuTopBackgroundCats";
    public static final String key_chats_menuTopBackground = "chats_menuTopBackground";
    public static final String key_chats_menuCloud = "chats_menuCloud";
    public static final String key_chats_menuCloudBackgroundCats = "chats_menuCloudBackgroundCats";
    public static final String key_chats_actionIcon = "chats_actionIcon";
    public static final String key_chats_actionBackground = "chats_actionBackground";
    public static final String key_chats_actionPressedBackground = "chats_actionPressedBackground";
    public static final String key_chats_actionUnreadIcon = "chats_actionUnreadIcon";
    public static final String key_chats_actionUnreadBackground = "chats_actionUnreadBackground";
    public static final String key_chats_actionUnreadPressedBackground = "chats_actionUnreadPressedBackground";
    public static final String key_chats_archivePullDownBackground = "chats_archivePullDownBackground";
    public static final String key_chats_archivePullDownBackgroundActive = "chats_archivePullDownBackgroundActive";
    public static final String key_chats_tabUnreadActiveBackground = "chats_tabUnreadActiveBackground";
    public static final String key_chats_tabUnreadUnactiveBackground = "chats_tabUnreadUnactiveBackground";

    public static final String key_chat_attachMediaBanBackground = "chat_attachMediaBanBackground";
    public static final String key_chat_attachMediaBanText = "chat_attachMediaBanText";
    public static final String key_chat_attachCheckBoxCheck = "chat_attachCheckBoxCheck";
    public static final String key_chat_attachCheckBoxBackground = "chat_attachCheckBoxBackground";
    public static final String key_chat_attachPhotoBackground = "chat_attachPhotoBackground";
    public static final String key_chat_attachActiveTab = "chat_attachActiveTab";
    public static final String key_chat_attachUnactiveTab = "chat_attachUnactiveTab";
    public static final String key_chat_attachPermissionImage = "chat_attachPermissionImage";
    public static final String key_chat_attachPermissionMark = "chat_attachPermissionMark";
    public static final String key_chat_attachPermissionText = "chat_attachPermissionText";
    public static final String key_chat_attachEmptyImage = "chat_attachEmptyImage";

    public static final String key_chat_inPollCorrectAnswer = "chat_inPollCorrectAnswer";
    public static final String key_chat_outPollCorrectAnswer = "chat_outPollCorrectAnswer";
    public static final String key_chat_inPollWrongAnswer = "chat_inPollWrongAnswer";
    public static final String key_chat_outPollWrongAnswer = "chat_outPollWrongAnswer";

    public static final String key_chat_attachGalleryBackground = "chat_attachGalleryBackground";
    public static final String key_chat_attachGalleryIcon = "chat_attachGalleryIcon";
    public static final String key_chat_attachGalleryText = "chat_attachGalleryText";
    public static final String key_chat_attachAudioBackground = "chat_attachAudioBackground";
    public static final String key_chat_attachAudioIcon = "chat_attachAudioIcon";
    public static final String key_chat_attachAudioText = "chat_attachAudioText";
    public static final String key_chat_attachFileBackground = "chat_attachFileBackground";
    public static final String key_chat_attachFileIcon = "chat_attachFileIcon";
    public static final String key_chat_attachFileText = "chat_attachFileText";
    public static final String key_chat_attachContactBackground = "chat_attachContactBackground";
    public static final String key_chat_attachContactIcon = "chat_attachContactIcon";
    public static final String key_chat_attachContactText = "chat_attachContactText";
    public static final String key_chat_attachLocationBackground = "chat_attachLocationBackground";
    public static final String key_chat_attachLocationIcon = "chat_attachLocationIcon";
    public static final String key_chat_attachLocationText = "chat_attachLocationText";
    public static final String key_chat_attachPollBackground = "chat_attachPollBackground";
    public static final String key_chat_attachPollIcon = "chat_attachPollIcon";
    public static final String key_chat_attachPollText = "chat_attachPollText";

    public static final String key_chat_status = "chat_status";
    public static final String key_chat_inRedCall = "chat_inUpCall";
    public static final String key_chat_inGreenCall = "chat_inDownCall";
    public static final String key_chat_outGreenCall = "chat_outUpCall";
    public static final String key_chat_inBubble = "chat_inBubble";
    public static final String key_chat_inBubbleSelected = "chat_inBubbleSelected";
    public static final String key_chat_inBubbleShadow = "chat_inBubbleShadow";
    public static final String key_chat_outBubble = "chat_outBubble";
    public static final String key_chat_outBubbleGradient = "chat_outBubbleGradient";
    public static final String key_chat_outBubbleGradientSelectedOverlay = "chat_outBubbleGradientSelectedOverlay";
    public static final String key_chat_outBubbleSelected = "chat_outBubbleSelected";
    public static final String key_chat_outBubbleShadow = "chat_outBubbleShadow";
    public static final String key_chat_messageTextIn = "chat_messageTextIn";
    public static final String key_chat_messageTextOut = "chat_messageTextOut";
    public static final String key_chat_messageLinkIn = "chat_messageLinkIn";
    public static final String key_chat_messageLinkOut = "chat_messageLinkOut";
    public static final String key_chat_serviceText = "chat_serviceText";
    public static final String key_chat_serviceLink = "chat_serviceLink";
    public static final String key_chat_serviceIcon = "chat_serviceIcon";
    public static final String key_chat_serviceBackground = "chat_serviceBackground";
    public static final String key_chat_serviceBackgroundSelected = "chat_serviceBackgroundSelected";
    public static final String key_chat_shareBackground = "chat_shareBackground";
    public static final String key_chat_shareBackgroundSelected = "chat_shareBackgroundSelected";
    public static final String key_chat_muteIcon = "chat_muteIcon";
    public static final String key_chat_lockIcon = "chat_lockIcon";
    public static final String key_chat_outSentCheck = "chat_outSentCheck";
    public static final String key_chat_outSentCheckSelected = "chat_outSentCheckSelected";
    public static final String key_chat_outSentCheckRead = "chat_outSentCheckRead";
    public static final String key_chat_outSentCheckReadSelected = "chat_outSentCheckReadSelected";
    public static final String key_chat_outSentClock = "chat_outSentClock";
    public static final String key_chat_outSentClockSelected = "chat_outSentClockSelected";
    public static final String key_chat_inSentClock = "chat_inSentClock";
    public static final String key_chat_inSentClockSelected = "chat_inSentClockSelected";
    public static final String key_chat_mediaSentCheck = "chat_mediaSentCheck";
    public static final String key_chat_mediaSentClock = "chat_mediaSentClock";
    public static final String key_chat_inMediaIcon = "chat_inMediaIcon";
    public static final String key_chat_outMediaIcon = "chat_outMediaIcon";
    public static final String key_chat_inMediaIconSelected = "chat_inMediaIconSelected";
    public static final String key_chat_outMediaIconSelected = "chat_outMediaIconSelected";
    public static final String key_chat_mediaTimeBackground = "chat_mediaTimeBackground";
    public static final String key_chat_outViews = "chat_outViews";
    public static final String key_chat_outViewsSelected = "chat_outViewsSelected";
    public static final String key_chat_inViews = "chat_inViews";
    public static final String key_chat_inViewsSelected = "chat_inViewsSelected";
    public static final String key_chat_mediaViews = "chat_mediaViews";
    public static final String key_chat_outMenu = "chat_outMenu";
    public static final String key_chat_outMenuSelected = "chat_outMenuSelected";
    public static final String key_chat_inMenu = "chat_inMenu";
    public static final String key_chat_inMenuSelected = "chat_inMenuSelected";
    public static final String key_chat_mediaMenu = "chat_mediaMenu";
    public static final String key_chat_outInstant = "chat_outInstant";
    public static final String key_chat_outInstantSelected = "chat_outInstantSelected";
    public static final String key_chat_inInstant = "chat_inInstant";
    public static final String key_chat_inInstantSelected = "chat_inInstantSelected";
    public static final String key_chat_sentError = "chat_sentError";
    public static final String key_chat_sentErrorIcon = "chat_sentErrorIcon";
    public static final String key_chat_selectedBackground = "chat_selectedBackground";
    public static final String key_chat_previewDurationText = "chat_previewDurationText";
    public static final String key_chat_previewGameText = "chat_previewGameText";
    public static final String key_chat_inPreviewInstantText = "chat_inPreviewInstantText";
    public static final String key_chat_outPreviewInstantText = "chat_outPreviewInstantText";
    public static final String key_chat_inPreviewInstantSelectedText = "chat_inPreviewInstantSelectedText";
    public static final String key_chat_outPreviewInstantSelectedText = "chat_outPreviewInstantSelectedText";
    public static final String key_chat_secretTimeText = "chat_secretTimeText";
    public static final String key_chat_stickerNameText = "chat_stickerNameText";
    public static final String key_chat_botButtonText = "chat_botButtonText";
    public static final String key_chat_botProgress = "chat_botProgress";
    public static final String key_chat_inForwardedNameText = "chat_inForwardedNameText";
    public static final String key_chat_outForwardedNameText = "chat_outForwardedNameText";
    public static final String key_chat_inPsaNameText = "chat_inPsaNameText";
    public static final String key_chat_outPsaNameText = "chat_outPsaNameText";
    public static final String key_chat_inViaBotNameText = "chat_inViaBotNameText";
    public static final String key_chat_outViaBotNameText = "chat_outViaBotNameText";
    public static final String key_chat_stickerViaBotNameText = "chat_stickerViaBotNameText";
    public static final String key_chat_inReplyLine = "chat_inReplyLine";
    public static final String key_chat_outReplyLine = "chat_outReplyLine";
    public static final String key_chat_stickerReplyLine = "chat_stickerReplyLine";
    public static final String key_chat_inReplyNameText = "chat_inReplyNameText";
    public static final String key_chat_outReplyNameText = "chat_outReplyNameText";
    public static final String key_chat_stickerReplyNameText = "chat_stickerReplyNameText";
    public static final String key_chat_inReplyMessageText = "chat_inReplyMessageText";
    public static final String key_chat_outReplyMessageText = "chat_outReplyMessageText";
    public static final String key_chat_inReplyMediaMessageText = "chat_inReplyMediaMessageText";
    public static final String key_chat_outReplyMediaMessageText = "chat_outReplyMediaMessageText";
    public static final String key_chat_inReplyMediaMessageSelectedText = "chat_inReplyMediaMessageSelectedText";
    public static final String key_chat_outReplyMediaMessageSelectedText = "chat_outReplyMediaMessageSelectedText";
    public static final String key_chat_stickerReplyMessageText = "chat_stickerReplyMessageText";
    public static final String key_chat_inPreviewLine = "chat_inPreviewLine";
    public static final String key_chat_outPreviewLine = "chat_outPreviewLine";
    public static final String key_chat_inSiteNameText = "chat_inSiteNameText";
    public static final String key_chat_outSiteNameText = "chat_outSiteNameText";
    public static final String key_chat_inContactNameText = "chat_inContactNameText";
    public static final String key_chat_outContactNameText = "chat_outContactNameText";
    public static final String key_chat_inContactPhoneText = "chat_inContactPhoneText";
    public static final String key_chat_inContactPhoneSelectedText = "chat_inContactPhoneSelectedText";
    public static final String key_chat_outContactPhoneText = "chat_outContactPhoneText";
    public static final String key_chat_outContactPhoneSelectedText = "chat_outContactPhoneSelectedText";
    public static final String key_chat_mediaProgress = "chat_mediaProgress";
    public static final String key_chat_inAudioProgress = "chat_inAudioProgress";
    public static final String key_chat_outAudioProgress = "chat_outAudioProgress";
    public static final String key_chat_inAudioSelectedProgress = "chat_inAudioSelectedProgress";
    public static final String key_chat_outAudioSelectedProgress = "chat_outAudioSelectedProgress";
    public static final String key_chat_mediaTimeText = "chat_mediaTimeText";
    public static final String key_chat_adminText = "chat_adminText";
    public static final String key_chat_adminSelectedText = "chat_adminSelectedText";
    public static final String key_chat_inTimeText = "chat_inTimeText";
    public static final String key_chat_outTimeText = "chat_outTimeText";
    public static final String key_chat_inTimeSelectedText = "chat_inTimeSelectedText";
    public static final String key_chat_outTimeSelectedText = "chat_outTimeSelectedText";
    public static final String key_chat_inAudioPerformerText = "chat_inAudioPerfomerText";
    public static final String key_chat_inAudioPerformerSelectedText = "chat_inAudioPerfomerSelectedText";
    public static final String key_chat_outAudioPerformerText = "chat_outAudioPerfomerText";
    public static final String key_chat_outAudioPerformerSelectedText = "chat_outAudioPerfomerSelectedText";
    public static final String key_chat_inAudioTitleText = "chat_inAudioTitleText";
    public static final String key_chat_outAudioTitleText = "chat_outAudioTitleText";
    public static final String key_chat_inAudioDurationText = "chat_inAudioDurationText";
    public static final String key_chat_outAudioDurationText = "chat_outAudioDurationText";
    public static final String key_chat_inAudioDurationSelectedText = "chat_inAudioDurationSelectedText";
    public static final String key_chat_outAudioDurationSelectedText = "chat_outAudioDurationSelectedText";
    public static final String key_chat_inAudioSeekbar = "chat_inAudioSeekbar";
    public static final String key_chat_inAudioCacheSeekbar = "chat_inAudioCacheSeekbar";
    public static final String key_chat_outAudioSeekbar = "chat_outAudioSeekbar";
    public static final String key_chat_outAudioCacheSeekbar = "chat_outAudioCacheSeekbar";
    public static final String key_chat_inAudioSeekbarSelected = "chat_inAudioSeekbarSelected";
    public static final String key_chat_outAudioSeekbarSelected = "chat_outAudioSeekbarSelected";
    public static final String key_chat_inAudioSeekbarFill = "chat_inAudioSeekbarFill";
    public static final String key_chat_outAudioSeekbarFill = "chat_outAudioSeekbarFill";
    public static final String key_chat_inVoiceSeekbar = "chat_inVoiceSeekbar";
    public static final String key_chat_outVoiceSeekbar = "chat_outVoiceSeekbar";
    public static final String key_chat_inVoiceSeekbarSelected = "chat_inVoiceSeekbarSelected";
    public static final String key_chat_outVoiceSeekbarSelected = "chat_outVoiceSeekbarSelected";
    public static final String key_chat_inVoiceSeekbarFill = "chat_inVoiceSeekbarFill";
    public static final String key_chat_outVoiceSeekbarFill = "chat_outVoiceSeekbarFill";
    public static final String key_chat_inFileProgress = "chat_inFileProgress";
    public static final String key_chat_outFileProgress = "chat_outFileProgress";
    public static final String key_chat_inFileProgressSelected = "chat_inFileProgressSelected";
    public static final String key_chat_outFileProgressSelected = "chat_outFileProgressSelected";
    public static final String key_chat_inFileNameText = "chat_inFileNameText";
    public static final String key_chat_outFileNameText = "chat_outFileNameText";
    public static final String key_chat_inFileInfoText = "chat_inFileInfoText";
    public static final String key_chat_outFileInfoText = "chat_outFileInfoText";
    public static final String key_chat_inFileInfoSelectedText = "chat_inFileInfoSelectedText";
    public static final String key_chat_outFileInfoSelectedText = "chat_outFileInfoSelectedText";
    public static final String key_chat_inFileBackground = "chat_inFileBackground";
    public static final String key_chat_outFileBackground = "chat_outFileBackground";
    public static final String key_chat_inFileBackgroundSelected = "chat_inFileBackgroundSelected";
    public static final String key_chat_outFileBackgroundSelected = "chat_outFileBackgroundSelected";
    public static final String key_chat_inVenueInfoText = "chat_inVenueInfoText";
    public static final String key_chat_outVenueInfoText = "chat_outVenueInfoText";
    public static final String key_chat_inVenueInfoSelectedText = "chat_inVenueInfoSelectedText";
    public static final String key_chat_outVenueInfoSelectedText = "chat_outVenueInfoSelectedText";
    public static final String key_chat_mediaInfoText = "chat_mediaInfoText";
    public static final String key_chat_linkSelectBackground = "chat_linkSelectBackground";
    public static final String key_chat_textSelectBackground = "chat_textSelectBackground";
    public static final String key_chat_wallpaper = "chat_wallpaper";
    public static final String key_chat_wallpaper_gradient_to = "chat_wallpaper_gradient_to";
    public static final String key_chat_wallpaper_gradient_rotation = "chat_wallpaper_gradient_rotation";
    public static final String key_chat_messagePanelBackground = "chat_messagePanelBackground";
    public static final String key_chat_messagePanelShadow = "chat_messagePanelShadow";
    public static final String key_chat_messagePanelText = "chat_messagePanelText";
    public static final String key_chat_messagePanelHint = "chat_messagePanelHint";
    public static final String key_chat_messagePanelCursor = "chat_messagePanelCursor";
    public static final String key_chat_messagePanelIcons = "chat_messagePanelIcons";
    public static final String key_chat_messagePanelSend = "chat_messagePanelSend";
    public static final String key_chat_messagePanelVoiceLock = "key_chat_messagePanelVoiceLock";
    public static final String key_chat_messagePanelVoiceLockBackground = "key_chat_messagePanelVoiceLockBackground";
    public static final String key_chat_messagePanelVoiceLockShadow = "key_chat_messagePanelVoiceLockShadow";
    public static final String key_chat_messagePanelVideoFrame = "chat_messagePanelVideoFrame";
    public static final String key_chat_topPanelBackground = "chat_topPanelBackground";
    public static final String key_chat_topPanelClose = "chat_topPanelClose";
    public static final String key_chat_topPanelLine = "chat_topPanelLine";
    public static final String key_chat_topPanelTitle = "chat_topPanelTitle";
    public static final String key_chat_topPanelMessage = "chat_topPanelMessage";
    public static final String key_chat_reportSpam = "chat_reportSpam";
    public static final String key_chat_addContact = "chat_addContact";
    public static final String key_chat_inLoader = "chat_inLoader";
    public static final String key_chat_inLoaderSelected = "chat_inLoaderSelected";
    public static final String key_chat_outLoader = "chat_outLoader";
    public static final String key_chat_outLoaderSelected = "chat_outLoaderSelected";
    public static final String key_chat_inLoaderPhoto = "chat_inLoaderPhoto";
    public static final String key_chat_inLoaderPhotoSelected = "chat_inLoaderPhotoSelected";
    public static final String key_chat_inLoaderPhotoIcon = "chat_inLoaderPhotoIcon";
    public static final String key_chat_inLoaderPhotoIconSelected = "chat_inLoaderPhotoIconSelected";
    public static final String key_chat_outLoaderPhoto = "chat_outLoaderPhoto";
    public static final String key_chat_outLoaderPhotoSelected = "chat_outLoaderPhotoSelected";
    public static final String key_chat_outLoaderPhotoIcon = "chat_outLoaderPhotoIcon";
    public static final String key_chat_outLoaderPhotoIconSelected = "chat_outLoaderPhotoIconSelected";
    public static final String key_chat_mediaLoaderPhoto = "chat_mediaLoaderPhoto";
    public static final String key_chat_mediaLoaderPhotoSelected = "chat_mediaLoaderPhotoSelected";
    public static final String key_chat_mediaLoaderPhotoIcon = "chat_mediaLoaderPhotoIcon";
    public static final String key_chat_mediaLoaderPhotoIconSelected = "chat_mediaLoaderPhotoIconSelected";
    public static final String key_chat_inLocationBackground = "chat_inLocationBackground";
    public static final String key_chat_inLocationIcon = "chat_inLocationIcon";
    public static final String key_chat_outLocationBackground = "chat_outLocationBackground";
    public static final String key_chat_outLocationIcon = "chat_outLocationIcon";
    public static final String key_chat_inContactBackground = "chat_inContactBackground";
    public static final String key_chat_inContactIcon = "chat_inContactIcon";
    public static final String key_chat_outContactBackground = "chat_outContactBackground";
    public static final String key_chat_outContactIcon = "chat_outContactIcon";
    public static final String key_chat_inFileIcon = "chat_inFileIcon";
    public static final String key_chat_inFileSelectedIcon = "chat_inFileSelectedIcon";
    public static final String key_chat_outFileIcon = "chat_outFileIcon";
    public static final String key_chat_outFileSelectedIcon = "chat_outFileSelectedIcon";
    public static final String key_chat_replyPanelIcons = "chat_replyPanelIcons";
    public static final String key_chat_replyPanelClose = "chat_replyPanelClose";
    public static final String key_chat_replyPanelName = "chat_replyPanelName";
    public static final String key_chat_replyPanelMessage = "chat_replyPanelMessage";
    public static final String key_chat_replyPanelLine = "chat_replyPanelLine";
    public static final String key_chat_searchPanelIcons = "chat_searchPanelIcons";
    public static final String key_chat_searchPanelText = "chat_searchPanelText";
    public static final String key_chat_secretChatStatusText = "chat_secretChatStatusText";
    public static final String key_chat_fieldOverlayText = "chat_fieldOverlayText";
    public static final String key_chat_stickersHintPanel = "chat_stickersHintPanel";
    public static final String key_chat_botSwitchToInlineText = "chat_botSwitchToInlineText";
    public static final String key_chat_unreadMessagesStartArrowIcon = "chat_unreadMessagesStartArrowIcon";
    public static final String key_chat_unreadMessagesStartText = "chat_unreadMessagesStartText";
    public static final String key_chat_unreadMessagesStartBackground = "chat_unreadMessagesStartBackground";
    public static final String key_chat_inlineResultIcon = "chat_inlineResultIcon";
    public static final String key_chat_emojiPanelBackground = "chat_emojiPanelBackground";
    public static final String key_chat_emojiPanelBadgeBackground = "chat_emojiPanelBadgeBackground";
    public static final String key_chat_emojiPanelBadgeText = "chat_emojiPanelBadgeText";
    public static final String key_chat_emojiSearchBackground = "chat_emojiSearchBackground";
    public static final String key_chat_emojiSearchIcon = "chat_emojiSearchIcon";
    public static final String key_chat_emojiPanelShadowLine = "chat_emojiPanelShadowLine";
    public static final String key_chat_emojiPanelEmptyText = "chat_emojiPanelEmptyText";
    public static final String key_chat_emojiPanelIcon = "chat_emojiPanelIcon";
    public static final String key_chat_emojiBottomPanelIcon = "chat_emojiBottomPanelIcon";
    public static final String key_chat_emojiPanelIconSelected = "chat_emojiPanelIconSelected";
    public static final String key_chat_emojiPanelStickerPackSelector = "chat_emojiPanelStickerPackSelector";
    public static final String key_chat_emojiPanelStickerPackSelectorLine = "chat_emojiPanelStickerPackSelectorLine";
    public static final String key_chat_emojiPanelBackspace = "chat_emojiPanelBackspace";
    public static final String key_chat_emojiPanelMasksIcon = "chat_emojiPanelMasksIcon";
    public static final String key_chat_emojiPanelMasksIconSelected = "chat_emojiPanelMasksIconSelected";
    public static final String key_chat_emojiPanelTrendingTitle = "chat_emojiPanelTrendingTitle";
    public static final String key_chat_emojiPanelStickerSetName = "chat_emojiPanelStickerSetName";
    public static final String key_chat_emojiPanelStickerSetNameHighlight = "chat_emojiPanelStickerSetNameHighlight";
    public static final String key_chat_emojiPanelStickerSetNameIcon = "chat_emojiPanelStickerSetNameIcon";
    public static final String key_chat_emojiPanelTrendingDescription = "chat_emojiPanelTrendingDescription";
    public static final String key_chat_botKeyboardButtonText = "chat_botKeyboardButtonText";
    public static final String key_chat_botKeyboardButtonBackground = "chat_botKeyboardButtonBackground";
    public static final String key_chat_botKeyboardButtonBackgroundPressed = "chat_botKeyboardButtonBackgroundPressed";
    public static final String key_chat_emojiPanelNewTrending = "chat_emojiPanelNewTrending";
    public static final String key_chat_messagePanelVoicePressed = "chat_messagePanelVoicePressed";
    public static final String key_chat_messagePanelVoiceBackground = "chat_messagePanelVoiceBackground";
    public static final String key_chat_messagePanelVoiceDelete = "chat_messagePanelVoiceDelete";
    public static final String key_chat_messagePanelVoiceDuration = "chat_messagePanelVoiceDuration";
    public static final String key_chat_recordedVoicePlayPause = "chat_recordedVoicePlayPause";
    public static final String key_chat_recordedVoiceProgress = "chat_recordedVoiceProgress";
    public static final String key_chat_recordedVoiceProgressInner = "chat_recordedVoiceProgressInner";
    public static final String key_chat_recordedVoiceDot = "chat_recordedVoiceDot";
    public static final String key_chat_recordedVoiceBackground = "chat_recordedVoiceBackground";
    public static final String key_chat_recordVoiceCancel = "chat_recordVoiceCancel";
    public static final String key_chat_recordTime = "chat_recordTime";
    public static final String key_chat_messagePanelCancelInlineBot = "chat_messagePanelCancelInlineBot";
    public static final String key_chat_gifSaveHintText = "chat_gifSaveHintText";
    public static final String key_chat_gifSaveHintBackground = "chat_gifSaveHintBackground";
    public static final String key_chat_goDownButton = "chat_goDownButton";
    public static final String key_chat_goDownButtonShadow = "chat_goDownButtonShadow";
    public static final String key_chat_goDownButtonIcon = "chat_goDownButtonIcon";
    public static final String key_chat_goDownButtonCounter = "chat_goDownButtonCounter";
    public static final String key_chat_goDownButtonCounterBackground = "chat_goDownButtonCounterBackground";
    public static final String key_chat_secretTimerBackground = "chat_secretTimerBackground";
    public static final String key_chat_secretTimerText = "chat_secretTimerText";
    public static final String key_chat_outTextSelectionHighlight = "chat_outTextSelectionHighlight";
    public static final String key_chat_inTextSelectionHighlight = "chat_inTextSelectionHighlight";
    public static final String key_chat_recordedVoiceHighlight = "key_chat_recordedVoiceHighlight";
    public static final String key_chat_TextSelectionCursor = "chat_TextSelectionCursor";

    public static final String key_passport_authorizeBackground = "passport_authorizeBackground";
    public static final String key_passport_authorizeBackgroundSelected = "passport_authorizeBackgroundSelected";
    public static final String key_passport_authorizeText = "passport_authorizeText";

    public static final String key_profile_creatorIcon = "profile_creatorIcon";
    public static final String key_profile_title = "profile_title";
    public static final String key_profile_actionIcon = "profile_actionIcon";
    public static final String key_profile_actionBackground = "profile_actionBackground";
    public static final String key_profile_actionPressedBackground = "profile_actionPressedBackground";
    public static final String key_profile_verifiedBackground = "profile_verifiedBackground";
    public static final String key_profile_verifiedCheck = "profile_verifiedCheck";
    public static final String key_profile_status = "profile_status";

    public static final String key_profile_tabText = "profile_tabText";
    public static final String key_profile_tabSelectedText = "profile_tabSelectedText";
    public static final String key_profile_tabSelectedLine = "profile_tabSelectedLine";
    public static final String key_profile_tabSelector = "profile_tabSelector";

    public static final String key_sharedMedia_startStopLoadIcon = "sharedMedia_startStopLoadIcon";
    public static final String key_sharedMedia_linkPlaceholder = "sharedMedia_linkPlaceholder";
    public static final String key_sharedMedia_linkPlaceholderText = "sharedMedia_linkPlaceholderText";
    public static final String key_sharedMedia_photoPlaceholder = "sharedMedia_photoPlaceholder";
    public static final String key_sharedMedia_actionMode = "sharedMedia_actionMode";

    public static final String key_featuredStickers_addedIcon = "featuredStickers_addedIcon";
    public static final String key_featuredStickers_buttonProgress = "featuredStickers_buttonProgress";
    public static final String key_featuredStickers_addButton = "featuredStickers_addButton";
    public static final String key_featuredStickers_addButtonPressed = "featuredStickers_addButtonPressed";
    public static final String key_featuredStickers_removeButtonText = "featuredStickers_removeButtonText";
    public static final String key_featuredStickers_buttonText = "featuredStickers_buttonText";
    public static final String key_featuredStickers_unread = "featuredStickers_unread";

    public static final String key_stickers_menu = "stickers_menu";
    public static final String key_stickers_menuSelector = "stickers_menuSelector";

    public static final String key_changephoneinfo_image = "changephoneinfo_image";
    public static final String key_changephoneinfo_image2 = "changephoneinfo_image2";

    public static final String key_groupcreate_hintText = "groupcreate_hintText";
    public static final String key_groupcreate_cursor = "groupcreate_cursor";
    public static final String key_groupcreate_sectionShadow = "groupcreate_sectionShadow";
    public static final String key_groupcreate_sectionText = "groupcreate_sectionText";
    public static final String key_groupcreate_spanText = "groupcreate_spanText";
    public static final String key_groupcreate_spanBackground = "groupcreate_spanBackground";
    public static final String key_groupcreate_spanDelete = "groupcreate_spanDelete";

    public static final String key_contacts_inviteBackground = "contacts_inviteBackground";
    public static final String key_contacts_inviteText = "contacts_inviteText";

    public static final String key_login_progressInner = "login_progressInner";
    public static final String key_login_progressOuter = "login_progressOuter";

    public static final String key_musicPicker_checkbox = "musicPicker_checkbox";
    public static final String key_musicPicker_checkboxCheck = "musicPicker_checkboxCheck";
    public static final String key_musicPicker_buttonBackground = "musicPicker_buttonBackground";
    public static final String key_musicPicker_buttonIcon = "musicPicker_buttonIcon";

    public static final String key_picker_enabledButton = "picker_enabledButton";
    public static final String key_picker_disabledButton = "picker_disabledButton";
    public static final String key_picker_badge = "picker_badge";
    public static final String key_picker_badgeText = "picker_badgeText";

    public static final String key_location_sendLocationBackground = "location_sendLocationBackground";
    public static final String key_location_sendLocationIcon = "location_sendLocationIcon";
    public static final String key_location_sendLocationText = "location_sendLocationText";
    public static final String key_location_sendLiveLocationBackground = "location_sendLiveLocationBackground";
    public static final String key_location_sendLiveLocationIcon = "location_sendLiveLocationIcon";
    public static final String key_location_sendLiveLocationText = "location_sendLiveLocationText";
    public static final String key_location_liveLocationProgress = "location_liveLocationProgress";
    public static final String key_location_placeLocationBackground = "location_placeLocationBackground";
    public static final String key_location_actionIcon = "location_actionIcon";
    public static final String key_location_actionActiveIcon = "location_actionActiveIcon";
    public static final String key_location_actionBackground = "location_actionBackground";
    public static final String key_location_actionPressedBackground = "location_actionPressedBackground";

    public static final String key_dialog_liveLocationProgress = "dialog_liveLocationProgress";

    public static final String key_files_folderIcon = "files_folderIcon";
    public static final String key_files_folderIconBackground = "files_folderIconBackground";
    public static final String key_files_iconText = "files_iconText";

    public static final String key_sessions_devicesImage = "sessions_devicesImage";

    public static final String key_calls_callReceivedGreenIcon = "calls_callReceivedGreenIcon";
    public static final String key_calls_callReceivedRedIcon = "calls_callReceivedRedIcon";

    public static final String key_undo_background = "undo_background";
    public static final String key_undo_cancelColor = "undo_cancelColor";
    public static final String key_undo_infoColor = "undo_infoColor";

    public static final String key_sheet_scrollUp = "key_sheet_scrollUp";
    public static final String key_sheet_other = "key_sheet_other";

    public static final String key_wallet_blackBackground = "wallet_blackBackground";
    public static final String key_wallet_graySettingsBackground = "wallet_graySettingsBackground";
    public static final String key_wallet_grayBackground = "wallet_grayBackground";
    public static final String key_wallet_whiteBackground = "wallet_whiteBackground";
    public static final String key_wallet_blackBackgroundSelector = "wallet_blackBackgroundSelector";
    public static final String key_wallet_whiteText = "wallet_whiteText";
    public static final String key_wallet_blackText = "wallet_blackText";
    public static final String key_wallet_statusText = "wallet_statusText";
    public static final String key_wallet_grayText = "wallet_grayText";
    public static final String key_wallet_grayText2 = "wallet_grayText2";
    public static final String key_wallet_greenText = "wallet_greenText";
    public static final String key_wallet_redText = "wallet_redText";
    public static final String key_wallet_dateText = "wallet_dateText";
    public static final String key_wallet_commentText = "wallet_commentText";
    public static final String key_wallet_releaseBackground = "wallet_releaseBackground";
    public static final String key_wallet_pullBackground = "wallet_pullBackground";
    public static final String key_wallet_buttonBackground = "wallet_buttonBackground";
    public static final String key_wallet_buttonPressedBackground = "wallet_buttonPressedBackground";
    public static final String key_wallet_buttonText = "wallet_buttonText";
    public static final String key_wallet_addressConfirmBackground = "wallet_addressConfirmBackground";

    //ununsed
    public static final String key_chat_outBroadcast = "chat_outBroadcast";
    public static final String key_chat_mediaBroadcast = "chat_mediaBroadcast";

    public static final String key_player_actionBar = "player_actionBar";
    public static final String key_player_actionBarSelector = "player_actionBarSelector";
    public static final String key_player_actionBarTitle = "player_actionBarTitle";
    public static final String key_player_actionBarTop = "player_actionBarTop";
    public static final String key_player_actionBarSubtitle = "player_actionBarSubtitle";
    public static final String key_player_actionBarItems = "player_actionBarItems";
    public static final String key_player_background = "player_background";
    public static final String key_player_time = "player_time";
    public static final String key_player_progressBackground = "player_progressBackground";
    public static final String key_player_progressBackground2 = "player_progressBackground2";
    public static final String key_player_progressCachedBackground = "key_player_progressCachedBackground";
    public static final String key_player_progress = "player_progress";
    public static final String key_player_button = "player_button";
    public static final String key_player_buttonActive = "player_buttonActive";

    public static final String key_statisticChartSignature = "statisticChartSignature";
    public static final String key_statisticChartSignatureAlpha = "statisticChartSignatureAlpha";
    public static final String key_statisticChartHintLine = "statisticChartHintLine";
    public static final String key_statisticChartActiveLine = "statisticChartActiveLine";
    public static final String key_statisticChartInactivePickerChart = "statisticChartInactivePickerChart";
    public static final String key_statisticChartActivePickerChart = "statisticChartActivePickerChart";
    public static final String key_statisticChartPopupBackground = "statisticChartPopupBackground";
    public static final String key_statisticChartRipple = "statisticChartRipple";
    public static final String key_statisticChartBackZoomColor = "statisticChartBackZoomColor";
    public static final String key_statisticChartCheckboxInactive = "statisticChartCheckboxInactive";
    public static final String key_statisticChartNightIconColor = "statisticChartNightIconColor";
    public static final String key_statisticChartChevronColor = "statisticChartChevronColor";
    public static final String key_statisticChartHighlightColor = "statisticChartHighlightColor";
    public final static String key_statisticChartLine_blue = "statisticChartLine_blue";
    public final static String key_statisticChartLine_green = "statisticChartLine_green";
    public final static String key_statisticChartLine_red = "statisticChartLine_red";
    public final static String key_statisticChartLine_golden = "statisticChartLine_golden";
    public final static String key_statisticChartLine_lightblue = "statisticChartLine_lightblue";
    public final static String key_statisticChartLine_lightgreen = "statisticChartLine_lightgreen";
    public final static String key_statisticChartLine_orange = "statisticChartLine_orange";
    public final static String key_statisticChartLine_indigo = "statisticChartLine_indigo";
    public final static String key_statisticChartLineEmpty = "statisticChartLineEmpty";

    private static HashSet<String> myMessagesColorKeys = new HashSet<>();
    private static HashMap<String, Integer> defaultColors = new HashMap<>();
    private static HashMap<String, String> fallbackKeys = new HashMap<>();
    private static HashSet<String> themeAccentExclusionKeys = new HashSet<>();
    private static HashMap<String, Integer> currentColorsNoAccent;
    private static HashMap<String, Integer> currentColors;
    private static HashMap<String, Integer> animatingColors;
    private static boolean shouldDrawGradientIcons;

    private static ThreadLocal<float[]> hsvTemp1Local = new ThreadLocal<>();
    private static ThreadLocal<float[]> hsvTemp2Local = new ThreadLocal<>();
    private static ThreadLocal<float[]> hsvTemp3Local = new ThreadLocal<>();
    private static ThreadLocal<float[]> hsvTemp4Local = new ThreadLocal<>();
    private static ThreadLocal<float[]> hsvTemp5Local = new ThreadLocal<>();


    private static Method StateListDrawable_getStateDrawableMethod;
    private static Field BitmapDrawable_mColorFilter;



    @SuppressLint("PrivateApi")
    private static Drawable getStateDrawable(Drawable drawable, int index) {
        if (Build.VERSION.SDK_INT >= 29 && drawable instanceof StateListDrawable) {
            return ((StateListDrawable) drawable).getStateDrawable(index);
        } else {
            if (StateListDrawable_getStateDrawableMethod == null) {
                try {
                    StateListDrawable_getStateDrawableMethod = StateListDrawable.class.getDeclaredMethod("getStateDrawable", int.class);
                } catch (Throwable ignore) {

                }
            }
            if (StateListDrawable_getStateDrawableMethod == null) {
                return null;
            }
            try {
                return (Drawable) StateListDrawable_getStateDrawableMethod.invoke(drawable, index);
            } catch (Exception ignore) {

            }
            return null;
        }
    }

    public static Drawable createEmojiIconSelectorDrawable(Context context, int resource, int defaultColor, int pressedColor) {
        Resources resources = context.getResources();
        Drawable defaultDrawable = resources.getDrawable(resource).mutate();
        if (defaultColor != 0) {
            defaultDrawable.setColorFilter(new PorterDuffColorFilter(defaultColor, PorterDuff.Mode.MULTIPLY));
        }
        Drawable pressedDrawable = resources.getDrawable(resource).mutate();
        if (pressedColor != 0) {
            pressedDrawable.setColorFilter(new PorterDuffColorFilter(pressedColor, PorterDuff.Mode.MULTIPLY));
        }
        StateListDrawable stateListDrawable = new StateListDrawable() {
            @Override
            public boolean selectDrawable(int index) {
                if (Build.VERSION.SDK_INT < 21) {
                    Drawable drawable = Theme.getStateDrawable(this, index);
                    ColorFilter colorFilter = null;
                    if (drawable instanceof BitmapDrawable) {
                        colorFilter = ((BitmapDrawable) drawable).getPaint().getColorFilter();
                    } else if (drawable instanceof NinePatchDrawable) {
                        colorFilter = ((NinePatchDrawable) drawable).getPaint().getColorFilter();
                    }
                    boolean result = super.selectDrawable(index);
                    if (colorFilter != null) {
                        drawable.setColorFilter(colorFilter);
                    }
                    return result;
                }
                return super.selectDrawable(index);
            }
        };
        stateListDrawable.setEnterFadeDuration(1);
        stateListDrawable.setExitFadeDuration(200);
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, pressedDrawable);
        stateListDrawable.addState(new int[]{}, defaultDrawable);
        return stateListDrawable;
    }



    public static boolean canStartHolidayAnimation() {
        return canStartHolidayAnimation;
    }

    public static int getEventType() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int monthOfYear = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int minutes = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        int eventType = -1;
        if (monthOfYear == 11 && dayOfMonth >= 24 && dayOfMonth <= 31 || monthOfYear == 0 && dayOfMonth == 1) {
            eventType = 0;
        } else if (monthOfYear == 1 && dayOfMonth == 14) {
            eventType = 1;
        }
        return eventType;
    }



    public static int getCurrentHolidayDrawableXOffset() {
        return dialogs_holidayDrawableOffsetX;
    }

    public static int getCurrentHolidayDrawableYOffset() {
        return dialogs_holidayDrawableOffsetY;
    }

    public static Drawable createSimpleSelectorDrawable(Context context, int resource, int defaultColor, int pressedColor) {
        Resources resources = context.getResources();
        Drawable defaultDrawable = resources.getDrawable(resource).mutate();
        if (defaultColor != 0) {
            defaultDrawable.setColorFilter(new PorterDuffColorFilter(defaultColor, PorterDuff.Mode.MULTIPLY));
        }
        Drawable pressedDrawable = resources.getDrawable(resource).mutate();
        if (pressedColor != 0) {
            pressedDrawable.setColorFilter(new PorterDuffColorFilter(pressedColor, PorterDuff.Mode.MULTIPLY));
        }
        StateListDrawable stateListDrawable = new StateListDrawable() {
            @Override
            public boolean selectDrawable(int index) {
                if (Build.VERSION.SDK_INT < 21) {
                    Drawable drawable = Theme.getStateDrawable(this, index);
                    ColorFilter colorFilter = null;
                    if (drawable instanceof BitmapDrawable) {
                        colorFilter = ((BitmapDrawable) drawable).getPaint().getColorFilter();
                    } else if (drawable instanceof NinePatchDrawable) {
                        colorFilter = ((NinePatchDrawable) drawable).getPaint().getColorFilter();
                    }
                    boolean result = super.selectDrawable(index);
                    if (colorFilter != null) {
                        drawable.setColorFilter(colorFilter);
                    }
                    return result;
                }
                return super.selectDrawable(index);
            }
        };
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, pressedDrawable);
        stateListDrawable.addState(StateSet.WILD_CARD, defaultDrawable);
        return stateListDrawable;
    }

    public static ShapeDrawable createCircleDrawable(int size, int color) {
        OvalShape ovalShape = new OvalShape();
        ovalShape.resize(size, size);
        ShapeDrawable defaultDrawable = new ShapeDrawable(ovalShape);
        defaultDrawable.setIntrinsicWidth(size);
        defaultDrawable.setIntrinsicHeight(size);
        defaultDrawable.getPaint().setColor(color);
        return defaultDrawable;
    }

    public static Drawable createSimpleSelectorCircleDrawable(int size, int defaultColor, int pressedColor) {
        OvalShape ovalShape = new OvalShape();
        ovalShape.resize(size, size);
        ShapeDrawable defaultDrawable = new ShapeDrawable(ovalShape);
        defaultDrawable.getPaint().setColor(defaultColor);
        ShapeDrawable pressedDrawable = new ShapeDrawable(ovalShape);
        if (Build.VERSION.SDK_INT >= 21) {
            pressedDrawable.getPaint().setColor(0xffffffff);
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{pressedColor}
            );
            return new RippleDrawable(colorStateList, defaultDrawable, pressedDrawable);
        } else {
            pressedDrawable.getPaint().setColor(pressedColor);
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
            stateListDrawable.addState(new int[]{android.R.attr.state_focused}, pressedDrawable);
            stateListDrawable.addState(StateSet.WILD_CARD, defaultDrawable);
            return stateListDrawable;
        }
    }

    public static Drawable createRoundRectDrawable(int rad, int defaultColor) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        defaultDrawable.getPaint().setColor(defaultColor);
        return defaultDrawable;
    }

    public static Drawable createSimpleSelectorRoundRectDrawable(int rad, int defaultColor, int pressedColor) {
        return createSimpleSelectorRoundRectDrawable(rad, defaultColor, pressedColor, pressedColor);
    }

    public static Drawable createSimpleSelectorRoundRectDrawable(int rad, int defaultColor, int pressedColor, int maskColor) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        defaultDrawable.getPaint().setColor(defaultColor);
        ShapeDrawable pressedDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        pressedDrawable.getPaint().setColor(maskColor);
        if (Build.VERSION.SDK_INT >= 21) {
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{pressedColor}
            );
            return new RippleDrawable(colorStateList, defaultDrawable, pressedDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, pressedDrawable);
            stateListDrawable.addState(StateSet.WILD_CARD, defaultDrawable);
            return stateListDrawable;
        }
    }

    public static Drawable createSelectorDrawableFromDrawables(Drawable normal, Drawable pressed) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressed);
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, pressed);
        stateListDrawable.addState(StateSet.WILD_CARD, normal);
        return stateListDrawable;
    }

    public static Drawable getRoundRectSelectorDrawable(int color) {
        if (Build.VERSION.SDK_INT >= 21) {
            Drawable maskDrawable = createRoundRectDrawable(AndroidUtilities.dp(3), 0xffffffff);
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{(color & 0x00ffffff) | 0x19000000}
            );
            return new RippleDrawable(colorStateList, null, maskDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, createRoundRectDrawable(AndroidUtilities.dp(3), (color & 0x00ffffff) | 0x19000000));
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, createRoundRectDrawable(AndroidUtilities.dp(3), (color & 0x00ffffff) | 0x19000000));
            stateListDrawable.addState(StateSet.WILD_CARD, new ColorDrawable(0x00000000));
            return stateListDrawable;
        }
    }

    public static Drawable createSelectorWithBackgroundDrawable(int backgroundColor, int color) {
        if (Build.VERSION.SDK_INT >= 21) {
            Drawable maskDrawable = new ColorDrawable(backgroundColor);
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{color}
            );
            return new RippleDrawable(colorStateList, new ColorDrawable(backgroundColor), maskDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(color));
            stateListDrawable.addState(StateSet.WILD_CARD, new ColorDrawable(backgroundColor));
            return stateListDrawable;
        }
    }

    public static Drawable createSelectorDrawable(int color) {
        return createSelectorDrawable(color, 1, -1);
    }

    public static Drawable createSelectorDrawable(int color, int maskType) {
        return createSelectorDrawable(color, maskType, -1);
    }

    public static Drawable createSelectorDrawable(int color, int maskType, int radius) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= 21) {
            Drawable maskDrawable = null;
            if ((maskType == 1 || maskType == 5) && Build.VERSION.SDK_INT >= 23) {
                maskDrawable = null;
            } else if (maskType == 1 || maskType == 3 || maskType == 4 || maskType == 5 || maskType == 6 || maskType == 7) {
                maskPaint.setColor(0xffffffff);
                maskDrawable = new Drawable() {

                    RectF rect;

                    @Override
                    public void draw(Canvas canvas) {
                        android.graphics.Rect bounds = getBounds();
                        if (maskType == 7) {
                            if (rect == null) {
                                rect = new RectF();
                            }
                            rect.set(bounds);
                            canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), maskPaint);
                        } else {
                            int rad;
                            if (maskType == 1 || maskType == 6) {
                                rad = AndroidUtilities.dp(20);
                            } else if (maskType == 3) {
                                rad = (Math.max(bounds.width(), bounds.height()) / 2);
                            } else {
                                rad = (int) Math.ceil(Math.sqrt((bounds.left - bounds.centerX()) * (bounds.left - bounds.centerX()) + (bounds.top - bounds.centerY()) * (bounds.top - bounds.centerY())));
                            }
                            canvas.drawCircle(bounds.centerX(), bounds.centerY(), rad, maskPaint);
                        }
                    }

                    @Override
                    public void setAlpha(int alpha) {

                    }

                    @Override
                    public void setColorFilter(ColorFilter colorFilter) {

                    }

                    @Override
                    public int getOpacity() {
                        return PixelFormat.UNKNOWN;
                    }
                };
            } else if (maskType == 2) {
                maskDrawable = new ColorDrawable(0xffffffff);
            }
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{color}
            );
            RippleDrawable rippleDrawable = new RippleDrawable(colorStateList, null, maskDrawable);
            if (Build.VERSION.SDK_INT >= 23) {
                if (maskType == 1) {
                    rippleDrawable.setRadius(radius <= 0 ? AndroidUtilities.dp(20) : radius);
                } else if (maskType == 5) {
                    rippleDrawable.setRadius(RippleDrawable.RADIUS_AUTO);
                }
            }
            return rippleDrawable;
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(color));
            stateListDrawable.addState(StateSet.WILD_CARD, new ColorDrawable(0x00000000));
            return stateListDrawable;
        }
    }

    public static Drawable createRadSelectorDrawable(int color, int topRad, int bottomRad) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= 21) {
            maskPaint.setColor(0xffffffff);
            Drawable maskDrawable = new Drawable() {

                private Path path = new Path();
                private RectF rect = new RectF();
                private float[] radii = new float[8];

                @Override
                public void draw(Canvas canvas) {
                    radii[0] = radii[1] = radii[2] = radii[3] = AndroidUtilities.dp(topRad);
                    radii[4] = radii[5] = radii[6] = radii[7] = AndroidUtilities.dp(bottomRad);
                    rect.set(getBounds());
                    path.addRoundRect(rect, radii, Path.Direction.CW);
                    canvas.drawPath(path, maskPaint);
                }

                @Override
                public void setAlpha(int alpha) {

                }

                @Override
                public void setColorFilter(ColorFilter colorFilter) {

                }

                @Override
                public int getOpacity() {
                    return PixelFormat.UNKNOWN;
                }
            };
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{color}
            );
            return new RippleDrawable(colorStateList, null, maskDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(color));
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(color));
            stateListDrawable.addState(StateSet.WILD_CARD, new ColorDrawable(0x00000000));
            return stateListDrawable;
        }
    }





    private static int changeBrightness(int color, float amount) {
        int r = (int) (Color.red(color) * amount);
        int g = (int) (Color.green(color) * amount);
        int b = (int) (Color.blue(color) * amount);

        r = r < 0 ? 0 : Math.min(r, 255);
        g = g < 0 ? 0 : Math.min(g, 255);
        b = b < 0 ? 0 : Math.min(b, 255);
        return Color.argb(Color.alpha(color), r, g, b);
    }



    public static HashMap<String, Integer> getDefaultColors() {
        return defaultColors;
    }


    private static long getAutoNightSwitchThemeDelay() {
        long newTime = SystemClock.elapsedRealtime();
        if (Math.abs(lastThemeSwitchTime - newTime) >= LIGHT_SENSOR_THEME_SWITCH_NEAR_THRESHOLD) {
            return LIGHT_SENSOR_THEME_SWITCH_DELAY;
        }
        return LIGHT_SENSOR_THEME_SWITCH_NEAR_DELAY;
    }

    private static final float MAXIMUM_LUX_BREAKPOINT = 500.0f;


    public static int getPreviewColor(HashMap<String, Integer> colors, String key) {
        Integer color = colors.get(key);
        if (color == null) {
            color = defaultColors.get(key);
        }
        return color;
    }



    private static ColorFilter currentShareColorFilter;
    private static int currentShareColorFilterColor;
    private static ColorFilter currentShareSelectedColorFilter;
    private static  int currentShareSelectedColorFilterColor;
    public static ColorFilter getShareColorFilter(int color, boolean selected) {
        if (selected) {
            if (currentShareSelectedColorFilter == null || currentShareSelectedColorFilterColor != color) {
                currentShareSelectedColorFilterColor = color;
                currentShareSelectedColorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
            return currentShareSelectedColorFilter;
        } else {
            if (currentShareColorFilter == null || currentShareColorFilterColor != color) {
                currentShareColorFilterColor = color;
                currentShareColorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
            return currentShareColorFilter;
        }
    }

    public static Drawable getThemedDrawable(Context context, int resId, int color) {
        if (context == null) {
            return null;
        }
        Drawable drawable = context.getResources().getDrawable(resId).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        return drawable;
    }

    public static int getDefaultColor(String key) {
        Integer value = defaultColors.get(key);
        if (value == null) {
            if (key.equals(key_chats_menuTopShadow) || key.equals(key_chats_menuTopBackground) || key.equals(key_chats_menuTopShadowCats)) {
                return 0;
            }
            return 0xffff0000;
        }
        return value;
    }

    public static boolean hasThemeKey(String key) {
        return currentColors.containsKey(key);
    }

    public static Integer getColorOrNull(String key) {
        Integer color = currentColors.get(key);
        if (color == null) {
            String fallbackKey = fallbackKeys.get(key);
            if (fallbackKey != null) {
                color = currentColors.get(key);
            }
            if (color == null) {
                color = defaultColors.get(key);
            }
        }
        if (color != null && (key_windowBackgroundWhite.equals(key) || key_windowBackgroundGray.equals(key) || key_actionBarDefault.equals(key) || key_actionBarDefaultArchived.equals(key))) {
            color |= 0xff000000;
        }
        return color;
    }

    public static void setAnimatingColor(boolean animating) {
        animatingColors = animating ? new HashMap<>() : null;
    }

    public static boolean isAnimatingColor() {
        return animatingColors != null;
    }

    public static void setAnimatedColor(String key, int value) {
        if (animatingColors == null) {
            return;
        }
        animatingColors.put(key, value);
    }



    public static Drawable getCachedWallpaper() {
        synchronized (wallpaperSync) {
            if (themedWallpaper != null) {
                return themedWallpaper;
            } else {
                return wallpaper;
            }
        }
    }

    public static Drawable getCachedWallpaperNonBlocking() {
        if (themedWallpaper != null) {
            return themedWallpaper;
        } else {
            return wallpaper;
        }
    }
}