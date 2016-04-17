/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hackar.ar.glars.glarss;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;
    private Paint mBackgroundPaint;

    private volatile Face mFace;
    private int mFaceId;
    private MainView.PersonInfo mPersonInfo;

    private boolean mIsRecognizing;
    private String mSpokenText;
    private String mBlinkingDots = "";

    private boolean mStopThread;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        //final int selectedColor = Color.WHITE;

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(Color.WHITE);

        mIdPaint = new Paint();
        mIdPaint.setColor(Color.WHITE);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(Color.WHITE);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setAlpha(150);
    }



    void setId(int id) {
        mFaceId = id;
    }

    void setPersonInfo(MainView.PersonInfo info) {
        mPersonInfo = info;
    }

    void setSpeech(String text) {
        Log.d("SPEECH", "Setting speech");
        mSpokenText = text;
        mIsRecognizing = false;
    }

    void setSpeechProcessing(boolean toggle) {
        mIsRecognizing = toggle;

        if (toggle) {
            mStopThread = false;
            new Thread(new Runnable() {
                public void run() {
                    Log.d("BLINKER", "Starting blinker");
                    while(true) {
                        try {
                            if (mIsRecognizing) {
                                Log.d("BLINKER", "Recognition works. Blink.");
                                if (mBlinkingDots.length() > 0) {
                                    mBlinkingDots = "";
                                    Thread.sleep(200);
                                } else {
                                    mBlinkingDots = "...";
                                    Thread.sleep(500);
                                }
                            } else {
                                mBlinkingDots = "";
                                Log.d("BLINKER", "No recognition. Turning off.");
                                Thread.sleep(500);
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (mStopThread) {
                            break;
                        }
                    }
                }
            }).start();
        } else {
            mStopThread = true;
        }
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;

        canvas.drawRect(right-20, bottom-200, right-20+400, bottom, mBackgroundPaint);

        if (mPersonInfo != null) {
            canvas.drawText(mPersonInfo.getName(), right, bottom-160, mIdPaint);
            canvas.drawText(mPersonInfo.getEmail(), right, bottom-110, mIdPaint);
            canvas.drawText(mPersonInfo.getText(), right, bottom-60, mIdPaint);
            canvas.drawText(mPersonInfo.getStatus(), right, bottom-10, mIdPaint);
        }

        canvas.drawRect(left-250, bottom+20, left+600, bottom+85, mBackgroundPaint);

        if (mSpokenText != null) {
            canvas.drawText(mSpokenText+mBlinkingDots, left-235, bottom+70, mIdPaint);
        } else {
            canvas.drawText(mBlinkingDots, left-235, bottom+70, mIdPaint);
        }

        canvas.drawLine(right-20, bottom-200, right-20, bottom, mBoxPaint);
    }
}