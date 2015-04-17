package com.minoon.partialhidelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.LinearLayout;

/**
 * コンテンツの一部分を隠すレイアウト.
 * フッターをクリックすることでコンテンツの'すべて表示'/'一部分非表示'を切り替えられる。
 * 参考本ソース: ExpandableLayout
 *
 * @see "https://github.com/traex/ExpandableLayout"
 */
public class PartialHideLayout extends LinearLayout {
    private static final String TAG = PartialHideLayout.class.getSimpleName();

    /**
     * イベントリスナー
     */
    public static interface EventListener {
        public void onClickFooter(PartialHideLayout expandableView, View footerView, View contentView);

        public void onExpand(PartialHideLayout expandableView, View footerView, View contentView);

        public void onCollapse(PartialHideLayout expandableView, View footerView, View contentView);

        public void onExpandAnimationInterpolated(PartialHideLayout expandableView, float animatedValue);
    }

    /**
     * 一部非表示状態におけるコンテンツの表示部分の高さを返すインターフェース
     */
    public static interface CollapseHeightCalculator {
        public int getCollapseHeight();
    }

    private Boolean isAnimationRunning = false;
    private Boolean isOpened = false;
    private boolean isFirst = true;

    private Integer duration;
    private View contentView;
    private View footerView;
    private EventListener mEventListener = sDummyListener;
    private CollapseHeightCalculator mCalculator = sDefaultCalculator;

    // Dummy Listenr
    private static EventListener sDummyListener = new EventListener() {
        @Override
        public void onClickFooter(PartialHideLayout expandableView, View footerView, View contentView) {/*Do nothing*/}

        @Override
        public void onExpand(PartialHideLayout expandableView, View footerView, View contentView) {/*Do nothing*/}

        @Override
        public void onCollapse(PartialHideLayout expandableView, View footerView, View contentView) {/*Do nothing*/}

        @Override
        public void onExpandAnimationInterpolated(PartialHideLayout expandableView, float animatedValue) {/*Do nothing*/}
    };

    // Dummy Calculator
    private static CollapseHeightCalculator sDefaultCalculator = new CollapseHeightCalculator() {
        @Override
        public int getCollapseHeight() {
            return 0;
        }
    };

    /// Constructor

    public PartialHideLayout(Context context) {
        this(context, null);
    }

    public PartialHideLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PartialHideLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }



    /**
     * Viewの初期化処理を行う
     *
     * @param context
     * @param attrs
     */
    private void init(final Context context, AttributeSet attrs) {
        this.setOrientation(LinearLayout.VERTICAL);

        // attrからFooterとContentのレイアウトリソースIDを収得
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PartialHideLayout);
        final int footerID = typedArray.getResourceId(R.styleable.PartialHideLayout_phl_footerLayout, -1);
        final int contentID = typedArray.getResourceId(R.styleable.PartialHideLayout_phl_contentLayout, -1);

        duration = typedArray.getInt(R.styleable.PartialHideLayout_phl_duration, getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));

        if (footerID == -1 || contentID == -1) {
            return;
        }

        // Footer / Content のViewを設定
        contentView = View.inflate(context, contentID, null);
        footerView = View.inflate(context, footerID, null);
        this.addView(contentView);
        this.addView(footerView);

        init();
    }

    public void init() {
        contentView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        footerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        footerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // リスナーが登録されていれば通知
                if (mEventListener != null) {
                    mEventListener.onClickFooter(PartialHideLayout.this, footerView, contentView);
                }

                if (!isAnimationRunning) {
                    if (isOpened) {
                        collapse(contentView);
                    } else {
                        expand(contentView);
                    }

                    isAnimationRunning = true;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isAnimationRunning = false;
                        }
                    }, duration);
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() < 2 || (footerView != null && contentView != null)) {
            return;
        }

        // Footer / Content のViewを設定
        contentView = getChildAt(0);
        footerView = getChildAt(1);

        init();
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // 初回時のみ折りたたむ.
        if(isFirst) {
            isFirst = false;
            hide();
        }

    }

    /**
     * イベントリスナーを登録する。
     *
     * @param listener
     */
    public void setEventListener(EventListener listener) {
        mEventListener = listener == null ? sDummyListener : listener;
    }

    public void expand(final View v) {
        // イベント通知: expand
        mEventListener.onExpand(PartialHideLayout.this, footerView, contentView);

        // 対象の高さを取得
        // MeasureSpec.makeMeasureSpec(LayoutParams.MATCH_PARENT, MeasureSpec.EXACTLY);だとおかしくなる
        int widthMeasuredSpec = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
        v.measure(widthMeasuredSpec,  LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();
        final int collapsedHeight = getCollapsedHeight();
//        Log.d(TAG, "expand: targetHeight = " + targetHeight + ", collapsedHeight = " + collapsedHeight);

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    isOpened = true;
                }
                v.getLayoutParams().height = (interpolatedTime == 1) ? LayoutParams.WRAP_CONTENT : (int) ((targetHeight - collapsedHeight) * interpolatedTime + collapsedHeight);
                v.requestLayout();
            }


            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration(duration);
        animation.setInterpolator(new DecelerateInterpolator());
        v.startAnimation(animation);
    }

    /**
     * コンテンツを折りたたむ
     *
     * @param v
     */
    private void collapse(final View v) {
        // イベント通知: collapse
        mEventListener.onCollapse(PartialHideLayout.this, footerView, contentView);

        // 移動距離算出のためにViewの高さを取得
        final int initialHeight = v.getMeasuredHeight();
        final int collapsedHeight = getCollapsedHeight();

//        Log.d(TAG, "collapse: initialHeight = " + initialHeight + ", collapsedHeight = " + collapsedHeight);

        if (initialHeight < collapsedHeight) {
            // 現在のViewの高さが折りたたんだ時の高さより小さければ、おりたたんだ時の高さに設定して終了
            v.getLayoutParams().height = collapsedHeight;
            v.requestLayout();
            return;
        }

        if(v.isLayoutRequested()) {
            // まだレイアウトが完了していない（子Viewへのレイアウトリクエストの必要性 = true）-> 高さを設定して終了
//            Log.d(TAG, "collapse: no animation because not layouted.");
            v.getLayoutParams().height = collapsedHeight;
            v.requestLayout();
            return;
        }

        // 折りたたみアニメーション作成
//        Log.d(TAG, "collapse: start animation.");
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    // アニメーション終了
                    isOpened = false;
                }
                // 高さを変更
                v.getLayoutParams().height = initialHeight - (int)((initialHeight - collapsedHeight) * interpolatedTime);
                v.requestLayout();
//                Log.d(TAG, "collapse: v.height = " + v.getLayoutParams().height);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(duration);
        animation.setInterpolator(new DecelerateInterpolator());
        v.startAnimation(animation);
    }

    public Boolean isOpened() {
        return isOpened;
    }

    /**
     * コンテンツを広げる
     */
    public void show() {
        if (!isAnimationRunning) {
            expand(contentView);
            isAnimationRunning = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isAnimationRunning = false;
                }
            }, duration);
        }
    }

    /**
     * コンテンツを折りたたむ
     */
    public void hide() {
        if (!isAnimationRunning) {
            collapse(contentView);
            isAnimationRunning = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isAnimationRunning = false;
                }
            }, duration);
        }
    }

    public View getFooterView() {
        return footerView;
    }

    public View getContentLayout() {
        return contentView;
    }

    public void setAnimationDuration(int duration) {
        this.duration = duration;
    }

    public int getAnimationDuration() {
        return duration;
    }

    public void setCollapseHeightCalculator(CollapseHeightCalculator calculator) {
        mCalculator = calculator == null ? sDefaultCalculator : calculator;
    }

    private int getCollapsedHeight() {
        return mCalculator.getCollapseHeight();
    }
}
