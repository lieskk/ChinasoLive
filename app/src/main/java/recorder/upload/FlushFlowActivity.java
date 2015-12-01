package recorder.upload;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chinaso.cl.R;
import com.chinaso.cl.Utils.BezierEvaluator;
import com.chinaso.cl.Utils.RongUtil;
import com.letv.recorder.controller.LetvPublisher;
import com.letv.recorder.ui.RecorderSkin;
import com.letv.recorder.ui.RecorderView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Random;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;
import recorder.net.NetworkService;
import recorder.net.model.CoverInfo;
import recorder.net.model.StopVideoInfo;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

public class FlushFlowActivity extends Activity implements RongIMClient.OnReceiveMessageListener{

	protected static final String TAG = "FlushFlowActivity";
	
	private static LetvPublisher publisher;
	private RecorderView rv;
	private RecorderSkin recorderSkin;
	private String mActivityId;
	private TextView msg_count_text;
	private TextView msg_number_text;
	private TextView msg_text_show;
	private ImageView msg_like_show;

	private int msg_count=0;
	private int msg_number=0;

	private int SX, SY;
	private int DX ,DY;
	public static int DIS_X=100;
	public static int DIS_Y=800;
	public static int DELAY=500;
	public static int ANIM_DURATION=1000;
	private RelativeLayout container;
	private int[] heartIds=new int[]{R.mipmap.heart0,R.mipmap.heart1,R.mipmap.heart2,
			R.mipmap.heart3,R.mipmap.heart4,R.mipmap.heart5,
			R.mipmap.heart6,R.mipmap.heart7,R.mipmap.heart8,
			R.mipmap.heart9,R.mipmap.heart10,R.mipmap.heart11};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/**
		 * 全屏五毛特效
		 */
		Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		win.requestFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_recorder);
		Log.i("ly", "onCreate");
		
		rv = (RecorderView) findViewById(R.id.rv);//获取rootView
		msg_count_text = (TextView) findViewById(R.id.msg_like);
		msg_number_text= (TextView) findViewById(R.id.msg_number);
		msg_text_show= (TextView) findViewById(R.id.msg_text_show);
		msg_like_show= (ImageView) findViewById(R.id.msg_like_show);
		container= (RelativeLayout) findViewById(R.id.container);

		//createVideo();
		mActivityId=getIntent().getStringExtra("activityId");
		String userId = getIntent().getStringExtra("userId");
		String secretKey = getIntent().getStringExtra("secretKey");

		LetvPublisher.init(mActivityId, userId, secretKey);
		initPublish();//初始化推流器
		initSkin();//初始化皮肤
		bindingPublish();//绑定推流器

		RongIMClient.setOnReceiveMessageListener(FlushFlowActivity.this);
		RongUtil.initChatRoom(mActivityId, new RongIMClient.OperationCallback() {
			@Override
			public void onSuccess() {
				Log.i("ly", "init room success");
				msg_number++;
				FlushFlowActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						msg_count_text.setText("点赞数：" + msg_count);
						msg_number_text.setText("人数：" + msg_number);
					}
				});
				RongUtil.sendEnterMessage(0, msg_number, mActivityId);
			}

			@Override
			public void onError(RongIMClient.ErrorCode errorCode) {
				Log.e("ly", "init room error-->" + errorCode);
			}
		});

		uploadCover();
	}

//	private void createVideo(){
//		NetworkService.getInstance().createVideo("ly", new Callback<VideoIdInfo>() {
//			@Override
//			public void success(VideoIdInfo videoIdInfo, Response response) {
//				mActivityId = videoIdInfo.getLetvId();
//				//mActivityId = "A2015111900995";
//				String userId = getIntent().getStringExtra("userId");
//				String secretKey = getIntent().getStringExtra("secretKey");
//				LetvPublisher.init(mActivityId, userId, secretKey);
//
//				initPublish();//初始化推流器
//				initSkin();//初始化皮肤
//				bindingPublish();//绑定推流器
//			}
//
//			@Override
//			public void failure(RetrofitError retrofitError) {
//				Log.e("ly", "create video  err:" + retrofitError);
//			}
//		});
//
//	}
	private void stopVideo(){
		NetworkService.getInstance().stopVideo(mActivityId, new Callback<StopVideoInfo>() {
			@Override
			public void success(StopVideoInfo stopVideoInfo, Response response) {
				Log.i("ly", "delete video success");
			}

			@Override
			public void failure(RetrofitError retrofitError) {
				Log.e("ly", "delete video err " + retrofitError);
			}
		});
	}
	private void uploadCover(){

		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent,0);
		//NetworkService.getInstance().uploadFile(mActivityId,"videotitle",new );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Cursor cursor = getContentResolver().query(data.getData(), null, null, null, null);
		cursor.moveToFirst();
		int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
		String fileSrc = cursor.getString(idx);

		File cover=new File(fileSrc);
		Log.i("ly","file src-->"+fileSrc);

		String mimeType = "image/*";
		TypedFile fileToSend = new TypedFile(mimeType, cover);

		NetworkService.getInstance().uploadFile(mActivityId, "chinaso-video", fileToSend, new Callback<CoverInfo>() {
			@Override
			public void success(CoverInfo coverInfo, Response response) {
				Log.i("ly", "upload cover success");
			}

			@Override
			public void failure(RetrofitError retrofitError) {
				Log.e("ly", "upload cover err-->" + retrofitError);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		/**
		 * onResume的时候需要做一些事情
		 */
		Log.i("ly","onResume");
		if (recorderSkin != null) {
			recorderSkin.onResume();
		}

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				int[] location = new int[2];
				msg_like_show.getLocationOnScreen(location);
				int x = location[0];
				int y = location[1];
				SX = x;
				SY = y;
				DX=SX+DIS_X;
				DY=SY-DIS_Y;
				Log.i("ly","SX,SY = "+SX+","+SY);
			}
		}, DELAY);

	}

	@Override
	protected void onPause() {
		super.onPause();
		/**
		 * onPause的时候要作的一些事情
		 */
		Log.i("ly","onPause");
		if (recorderSkin != null) {
			recorderSkin.onPause();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i("ly", "onDestroy");
		if(recorderSkin!=null){
			recorderSkin.onDestroy();
		}
		stopVideo();
		msg_number--;
		RongUtil.quitChatRoom(mActivityId,msg_count,msg_number);
		//RongIMClient.getInstance().logout();
	}

	/**
	 * 初始化推流器
	 */
	private void initPublish() {
		publisher = LetvPublisher.getInstance();
		publisher.initPublisher(this);
	}
	/**
	 * 初始化皮肤
	 */
	private void initSkin() {
		recorderSkin = new RecorderSkin();
		recorderSkin.build(this, rv);
	}
	/**
	 * 皮肤于推流器关联
	 */
	private void bindingPublish() {
		recorderSkin.BindingPublisher(publisher);
		publisher.setCameraView(recorderSkin.getCameraView());
	}

	@Override
	public boolean onReceived(Message message, int i) {
		MessageContent messageContent = message.getContent();


		if (messageContent instanceof TextMessage) {//文本消息
			TextMessage textMessage = (TextMessage) messageContent;
			Log.d("ly", "onReceived-TextMessage:" + textMessage.getContent());
			//msg_count.setText(textMessage.getContent());
			try {
				final JSONObject ret=new JSONObject(textMessage.getContent());
				final String type=ret.getString("type");

				msg_count=ret.getInt("count");
				msg_number=ret.getInt("number");
				FlushFlowActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						msg_count_text.setText("点赞数：" + msg_count);
						msg_number_text.setText("人数：" + msg_number);
						if(type.equals("Text")){
							msg_text_show.setText(ret.optString("message"));
						}
						if(type.equals("Like")){
							excuteAnimation();
						}
					}
				});


			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.d("ly", "onReceived-其他消息，自己来判断处理");
		}

		return false;
	}

	private void excuteAnimation() {
		final ImageView iv = new ImageView(FlushFlowActivity.this);
		Random random = new Random();
		int index = random.nextInt(heartIds.length);
		iv.setImageDrawable(getResources().getDrawable(heartIds[index]));
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_RIGHT, R.id.msg_like_show);
		lp.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.msg_like_show);
		container.addView(iv, lp);

		final ValueAnimator valueAnimator = ValueAnimator.ofObject(new BezierEvaluator(SX,SY,DX,DY), new PointF(SX, SY), new PointF(DX, DY));
		valueAnimator.setDuration(ANIM_DURATION);
		valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				PointF pointF = (PointF) animation.getAnimatedValue();
				Log.i("ly", "pointF-->" + pointF.x + "," + pointF.y);

				iv.setX(pointF.x);
				iv.setY(pointF.y);
				iv.setAlpha((pointF.y - DY) / (SY - DY));
				iv.setScaleX((pointF.y - DY) / (SY - DY));
				iv.setScaleY((pointF.y - DY) / (SY - DY));

			}

		});

		valueAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				container.removeView(iv);
				iv.setAlpha(0);
				valueAnimator.cancel();
			}
		});
		valueAnimator.start();
	}

}
