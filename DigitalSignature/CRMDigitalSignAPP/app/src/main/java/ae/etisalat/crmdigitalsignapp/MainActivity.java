package ae.etisalat.crmdigitalsignapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button btn_get_sign, mClear, mGetSign, mCancel;

    LinearLayout mContent;
    View view;
    Signature mSignature;
    Bitmap bitmap;

    Map<String, String> crmDataMap;
    private String crmEndPointURl;
    private String signPadHaderDynimicMsg = "I have read and agree to the full set of terms and conditions.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if(intent != null) {
            String action = intent.getAction();
            Log.i("CRMSignIntent", action);
            Uri data = intent.getData();
            if(data != null) {
                // data.getPath() -> /eRetailer/etisalat/eSign
                // data.getQuery() -> redirecturl=http://195.229.186.224:4445/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=signature&user_id=CSSQA17&SUB_REQUEST=10558928&AttachmentType=SIGN&ApplType=PREPAID_AN&TASK_ID=CSSQA17-1548238190598
                // data.getQueryParameterNames().toString() -> [redirecturl, user_id, SUB_REQUEST, AttachmentType, ApplType, TASK_ID]
                // data.getQueryParameter("redirecturl")  -->>  http://195.229.186.224:4445/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=signature

                this.crmEndPointURl = data.getQueryParameter("redirecturl");
                Set<String> queryParameters = data.getQueryParameterNames();

                crmDataMap = new HashMap<String, String>();
                for (String s : queryParameters) {
                    System.out.println(s);
                    if(s.equalsIgnoreCase("redirecturl")
                            || s.equalsIgnoreCase("CRM_DYNAMIC_MSG")){
                        continue;
                    }
                    if(s.equalsIgnoreCase("CRM_DYNAMIC_DIS_FLAG")
                            && "Y".equalsIgnoreCase(data.getQueryParameter("CRM_DYNAMIC_DIS_FLAG")))
                    {
                        this.signPadHaderDynimicMsg = data.getQueryParameter("CRM_DYNAMIC_MSG");
                        continue;
                    }
                    crmDataMap.put(s,data.getQueryParameter(s));
                }
            }
        }

        mContent = (LinearLayout) findViewById(R.id.linearLayout);
        mSignature = new Signature(getApplicationContext(), null);
        mSignature.setBackgroundColor(Color.WHITE);
        // Dynamically generating Layout through java code
        mContent.addView(mSignature, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mClear = (Button) findViewById(R.id.clear);
        mGetSign = (Button) findViewById(R.id.getsign);
        mGetSign.setEnabled(false);
        mCancel = (Button) findViewById(R.id.cancel);

        TextView signPadHaderDynimicMsg = (TextView) findViewById(R.id.signPadHaderDynimicMsg);
        signPadHaderDynimicMsg.setText(this.signPadHaderDynimicMsg);

        view = mContent;

        mClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("log_tag", "Panel Cleared");
                mSignature.clear();
                mGetSign.setEnabled(false);
            }
        });

        mGetSign.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                Log.v("log_tag", "Panel Saved");
                view.setDrawingCacheEnabled(true);
                mSignature.save(view, "");
                Toast.makeText(getApplicationContext(), "Successfully Saved", Toast.LENGTH_SHORT).show();
                // Calling the same class
                recreate();
                finish();

            }
        });


        mCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("log_tag", "Panel Canceled");
                // Calling the same class
                recreate();
                finish();
            }
        });


    }

    public class Signature extends View {

        private static final float STROKE_WIDTH = 5f;
        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
        private Paint paint = new Paint();
        private Path path = new Path();

        private float lastTouchX;
        private float lastTouchY;
        private final RectF dirtyRect = new RectF();

        public Signature(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
        }

        public void save(View v, String StoredPath) {
            Log.v("log_tag", "Width: " + v.getWidth());
            Log.v("log_tag", "Height: " + v.getHeight());
            if(v.getWidth() <= 0 || v.getHeight() <= 0){
                return;
            }
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(mContent.getWidth(), mContent.getHeight(), Bitmap.Config.RGB_565);
            }
            Canvas canvas = new Canvas(bitmap);
            v.draw(canvas);

            // Send Sign Image to CRM.
            prepareHttpPostReq();

            if(false) { // To Create Image locally in Device. Dont unComment.
                try {
                    // Output the file
                    FileOutputStream mFileOutStream = new FileOutputStream(StoredPath);

                    // Convert the output file to Image such as .png
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);
                    mFileOutStream.flush();
                    mFileOutStream.close();

                } catch (Exception e) {
                    Log.v("log_tag", e.toString());
                }
            }

        }

        public void clear() {
            path.reset();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            mGetSign.setEnabled(true);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:

                    resetDirtyRect(eventX, eventY);
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++) {
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);
                    }
                    path.lineTo(eventX, eventY);
                    break;

                default:
                    debug("Ignored touch event: " + event.toString());
                    return false;
            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;

            return true;
        }

        private void debug(String string) {

            Log.v("log_tag", string);

        }

        private void expandDirtyRect(float historicalX, float historicalY) {
            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX;
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX;
            }

            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY;
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY) {
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }
    }

    private void prepareHttpPostReq()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();

        String photo = Base64.encodeToString(b, Base64.DEFAULT);
        this.crmDataMap.put("photo",photo);

        String sendHttpRequestDataArry[] = {
                crmEndPointURl,
                createQueryStringForParameters(this.crmDataMap)
        };
        new MainActivity.SendHttpRequestTask().execute(sendHttpRequestDataArry);
    }

    private static final char PARAMETER_DELIMITER = '&';
    private static final char PARAMETER_EQUALS_CHAR = '=';

    public String createQueryStringForParameters(Map<String, String> parameters) {
        StringBuilder parametersAsQueryString = new StringBuilder();
        if (parameters != null) {
            boolean firstParameter = true;

            for (String parameterName : parameters.keySet()) {
                if (!firstParameter) {
                    parametersAsQueryString.append(PARAMETER_DELIMITER);
                }

                try {
                    parametersAsQueryString.append(parameterName)
                            .append(PARAMETER_EQUALS_CHAR)
                            .append(URLEncoder.encode(parameters.get(parameterName), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                firstParameter = false;
            }
        }
        return parametersAsQueryString.toString();
    }

    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String returnResponse="FAILED";
            try {
                Log.i("SendHttpRequestTask", "doInBackground-Start");
                String endPointURl = strings[0];
                String postParameters = strings[1];
                Log.i("SendHttpRequestTask", "endPointURl- "+endPointURl);
                Log.i("SendHttpRequestTask", "postParameters- "+postParameters);
                URL url = null;
                try {
                    url = new URL(endPointURl);
                    HttpURLConnection client = null;
                    Log.i("HttpURLConnection", "Start");
                    try {
                        client = (HttpURLConnection) url.openConnection();
                        client.setRequestMethod("POST");
                        client.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        client.setDoOutput(true);

                        client.setFixedLengthStreamingMode(postParameters.length());
                        //client.setChunkedStreamingMode(0);
                        Log.i("HttpURLConnection", "SetFixedLengthStreamingMode");

                        OutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
                        Log.i("HttpURLConnection", "OutputStreamCreated");

                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputPost);
                        Log.i("HttpURLConnection", "OutputStreamWriterCreated");

                        outputStreamWriter.write(postParameters);
                        Log.i("HttpURLConnection", "Wrote Data on OutputStreamWriter!");

                        outputStreamWriter.flush();
                        Log.i("HttpURLConnection", "DataFlushed on OutputStreamWriter!");
                        outputStreamWriter.close();
                        Log.i("HttpURLConnection", "OutputStreamWriter CLOSED!");
                        outputPost.close();
                        Log.i("HttpURLConnection", "OutputStream CLOSED!");

                        int statusCode = client.getResponseCode();
                        Log.i("HttpURLConnection", "statusCode- "+statusCode);
                        if(statusCode == HttpURLConnection.HTTP_OK){
                            returnResponse="SUCCESS";
                            Log.i("HttpURLConnection", "Post parameter sent successfully!");
                        }
                        else{
                            returnResponse="FAILED";
                            Log.i("HttpURLConnection", "Failed to Post parameter to servers!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("HttpURLConnection", e.getMessage());
                        Log.i("SendHttpRequestTask", "ExceptionInnerTry");
                    } finally {
                        if (client != null)
                            client.disconnect();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.i("SendHttpRequestTask", "MalformedURLException");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i("SendHttpRequestTask", "ExceptionMainTry");
            }
            Log.i("SendHttpRequestTask", "doInBackground-End");
            return returnResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
}
