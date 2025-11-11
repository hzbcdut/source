/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.view;

import static android.Manifest.permission.HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
import static android.Manifest.permission.HIDE_OVERLAY_WINDOWS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.annotation.ColorInt;
import android.annotation.DrawableRes;
import android.annotation.FlaggedApi;
import android.annotation.FloatRange;
import android.annotation.IdRes;
import android.annotation.LayoutRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.StyleRes;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.annotation.UiContext;
import android.app.WindowConfiguration;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Pair;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.accessibility.AccessibilityEvent;
import android.window.OnBackInvokedDispatcher;

import java.util.Collections;
import java.util.List;

/**
 * 顶层窗口外观和行为策略的抽象基类。此类的实例应该作为添加到窗口管理器的顶层视图。
 * 它提供了标准的UI策略，如背景、标题区域、默认按键处理等。
 *
 * <p>框架将代表应用程序实例化此类的实现。
 */
public abstract class Window {
    /** "选项面板"功能的标志。默认启用。 */
    public static final int FEATURE_OPTIONS_PANEL = 0;
    /** "无标题"功能的标志，关闭屏幕顶部的标题。 */
    public static final int FEATURE_NO_TITLE = 1;

    /**
     * 进度指示器功能的标志。
     *
     * @deprecated 从API 21开始不再支持。
     */
    @Deprecated
    public static final int FEATURE_PROGRESS = 2;

    /** 标题栏左侧有图标的标志 */
    public static final int FEATURE_LEFT_ICON = 3;
    /** 标题栏右侧有图标的标志 */
    public static final int FEATURE_RIGHT_ICON = 4;

    /**
     * 不确定进度功能的标志。
     *
     * @deprecated 从API 21开始不再支持。
     */
    @Deprecated
    public static final int FEATURE_INDETERMINATE_PROGRESS = 5;

    /** 上下文菜单的标志。默认启用。 */
    public static final int FEATURE_CONTEXT_MENU = 6;
    /** 自定义标题的标志。不能与其他标题功能组合使用。 */
    public static final int FEATURE_CUSTOM_TITLE = 7;
    /**
     * 启用操作栏的标志。
     * 在某些设备上默认启用。操作栏替换标题栏，并在某些设备上为屏幕菜单按钮提供替代位置。
     */
    public static final int FEATURE_ACTION_BAR = 8;
    /**
     * 请求覆盖窗口内容的操作栏。
     * 通常操作栏会位于窗口内容上方的空间中，但如果与此功能一起请求{@link #FEATURE_ACTION_BAR}，
     * 它将覆盖在窗口内容本身上。如果您希望应用程序对操作栏的显示方式有更多的控制，
     * 例如让应用程序内容在具有透明背景的操作栏下滚动，或者以其他方式在应用程序内容上显示透明/半透明的操作栏，
     * 这将非常有用。
     *
     * <p>此模式特别适用于{@link View#SYSTEM_UI_FLAG_FULLSCREEN View.SYSTEM_UI_FLAG_FULLSCREEN}，
     * 它允许您无缝隐藏操作栏以及其他屏幕装饰。
     *
     * <p>从{@link android.os.Build.VERSION_CODES#JELLY_BEAN}开始，当操作栏处于此模式时，
     * 它将调整提供给{@link View#fitSystemWindows(android.graphics.Rect) View.fitSystemWindows(Rect)}的插入，
     * 以包括操作栏覆盖的内容，因此您可以在该空间内进行布局。
     */
    public static final int FEATURE_ACTION_BAR_OVERLAY = 9;
    /**
     * 指定在不存在操作栏时操作模式的行为的标志。
     * 如果启用了覆盖，操作模式UI将被允许覆盖现有窗口内容。
     */
    public static final int FEATURE_ACTION_MODE_OVERLAY = 10;
    /**
     * 请求一个无装饰的窗口，可通过从左侧滑动来关闭。
     *
     * @deprecated 滑动关闭功能不再有效。
     */
    @Deprecated
    public static final int FEATURE_SWIPE_TO_DISMISS = 11;
    /**
     * 请求使用TransitionManager对窗口内容更改进行动画处理。
     *
     * <p>TransitionManager使用{@link #setTransitionManager(android.transition.TransitionManager)}设置。
     * 如果未设置，则将使用默认的TransitionManager。</p>
     *
     * @see #setContentView
     */
    public static final int FEATURE_CONTENT_TRANSITIONS = 12;

    /**
     * 使活动能够通过发送或接收使用
     * {@link android.app.ActivityOptions#makeSceneTransitionAnimation(android.app.Activity,
     * android.util.Pair[])}或{@link android.app.ActivityOptions#makeSceneTransitionAnimation(
     * android.app.Activity, View, String)}创建的ActivityOptions包来运行活动转换。
     */
    public static final int FEATURE_ACTIVITY_TRANSITIONS = 13;

    /**
     * 用作功能ID的最大值
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final int FEATURE_MAX = FEATURE_ACTIVITY_TRANSITIONS;

    /**
     * 设置进度条可见性为VISIBLE的标志。
     *
     * @deprecated {@link #FEATURE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_VISIBILITY_ON = -1;

    /**
     * 设置进度条可见性为GONE的标志。
     *
     * @deprecated {@link #FEATURE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_VISIBILITY_OFF = -2;

    /**
     * 设置进度条不确定模式开启的标志。
     *
     * @deprecated {@link #FEATURE_INDETERMINATE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_INDETERMINATE_ON = -3;

    /**
     * 设置进度条不确定模式关闭的标志。
     *
     * @deprecated {@link #FEATURE_INDETERMINATE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_INDETERMINATE_OFF = -4;

    /**
     * (主)进度的起始值。
     *
     * @deprecated {@link #FEATURE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_START = 0;

    /**
     * (主)进度的结束值。
     *
     * @deprecated {@link #FEATURE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_END = 10000;

    /**
     * 次要进度的最低可能值。
     *
     * @deprecated {@link #FEATURE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_SECONDARY_START = 20000;

    /**
     * 次要进度的最高可能值。
     *
     * @deprecated {@link #FEATURE_PROGRESS}及相关方法从API 21开始不再支持。
     */
    @Deprecated
    public static final int PROGRESS_SECONDARY_END = 30000;

    /**
     * 当使用自定义背景时，状态栏背景视图的transitionName。
     * @see android.view.Window#setStatusBarColor(int)
     */
    public static final String STATUS_BAR_BACKGROUND_TRANSITION_NAME = "android:status:background";

    /**
     * 当使用自定义背景时，导航栏背景视图的transitionName。
     * @see android.view.Window#setNavigationBarColor(int)
     */
    public static final String NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME =
            "android:navigation:background";

    /**
     * 默认启用的功能。
     * @deprecated 请改用{@link #getDefaultFeatures(android.content.Context)}。
     */
    @Deprecated
    @SuppressWarnings({"PointlessBitwiseExpression"})
    protected static final int DEFAULT_FEATURES = (1 << FEATURE_OPTIONS_PANEL) |
            (1 << FEATURE_CONTEXT_MENU);

    /**
     * XML布局文件中主布局应具有的ID。
     */
    public static final int ID_ANDROID_CONTENT = com.android.internal.R.id.content;

    /**
     * 让主题驱动窗口标题控件颜色的标志。与{@link #setDecorCaptionShade(int)}一起使用。这是默认值。
     */
    public static final int DECOR_CAPTION_SHADE_AUTO = 0;
    /**
     * 设置窗口标题的浅色控件的标志。与{@link #setDecorCaptionShade(int)}一起使用。
     */
    public static final int DECOR_CAPTION_SHADE_LIGHT = 1;
    /**
     * 设置窗口标题的深色控件的标志。与{@link #setDecorCaptionShade(int)}一起使用。
     */
    public static final int DECOR_CAPTION_SHADE_DARK = 2;

    @UnsupportedAppUsage
    @UiContext
    private final Context mContext;

    @UnsupportedAppUsage
    private TypedArray mWindowStyle;
    @UnsupportedAppUsage
    private Callback mCallback;
    private OnWindowDismissedCallback mOnWindowDismissedCallback;
    private OnWindowSwipeDismissedCallback mOnWindowSwipeDismissedCallback;
    private WindowControllerCallback mWindowControllerCallback;
    @WindowInsetsController.Appearance
    private int mSystemBarAppearance;
    private DecorCallback mDecorCallback;
    private OnRestrictedCaptionAreaChangedListener mOnRestrictedCaptionAreaChangedListener;
    private Rect mRestrictedCaptionAreaRect;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private WindowManager mWindowManager;
    @UnsupportedAppUsage
    private IBinder mAppToken;
    @UnsupportedAppUsage
    private String mAppName;
    @UnsupportedAppUsage
    private boolean mHardwareAccelerated;
    private Window mContainer;
    private Window mActiveChild;
    private boolean mIsActive = false;
    private boolean mHasChildren = false;
    private boolean mCloseOnTouchOutside = false;
    private boolean mSetCloseOnTouchOutside = false;
    private int mForcedWindowFlags = 0;

    @UnsupportedAppUsage
    private int mFeatures;
    @UnsupportedAppUsage
    private int mLocalFeatures;

    private boolean mHaveWindowFormat = false;
    private boolean mHaveDimAmount = false;
    private int mDefaultWindowFormat = PixelFormat.OPAQUE;

    private boolean mHasSoftInputMode = false;

    @UnsupportedAppUsage
    private boolean mDestroyed;

    private boolean mOverlayWithDecorCaptionEnabled = true;
    private boolean mCloseOnSwipeEnabled = false;

    /**
     * 检查toolkitSetFrameRateReadOnly标志是否启用
     *
     * @hide
     */
    protected static boolean sToolkitSetFrameRateReadOnlyFlagValue =
                android.view.flags.Flags.toolkitSetFrameRateReadOnly();

    // 当前窗口属性。
    @UnsupportedAppUsage
    private final WindowManager.LayoutParams mWindowAttributes =
        new WindowManager.LayoutParams();

    /**
     * 从窗口回到其调用者的API。这允许客户端拦截按键分发、面板和菜单等。
     */
    public interface Callback {
        /**
         * 调用以处理按键事件。至少您的实现必须调用
         * {@link android.view.Window#superDispatchKeyEvent}来执行标准的按键处理。
         *
         * @param event 按键事件。
         *
         * @return boolean 如果此事件被消耗则返回true。
         */
        public boolean dispatchKeyEvent(KeyEvent event);

        /**
         * 调用以处理按键快捷方式事件。
         * 至少您的实现必须调用
         * {@link android.view.Window#superDispatchKeyShortcutEvent}来执行标准的按键快捷方式处理。
         *
         * @param event 按键快捷方式事件。
         * @return 如果此事件被消耗则返回true。
         */
        public boolean dispatchKeyShortcutEvent(KeyEvent event);

        /**
         * 调用以处理触摸屏事件。至少您的实现必须调用
         * {@link android.view.Window#superDispatchTouchEvent}来执行标准的触摸屏处理。
         *
         * @param event 触摸屏事件。
         *
         * @return boolean 如果此事件被消耗则返回true。
         */
        public boolean dispatchTouchEvent(MotionEvent event);

        /**
         * 调用以处理轨迹球事件。至少您的实现必须调用
         * {@link android.view.Window#superDispatchTrackballEvent}来执行标准的轨迹球处理。
         *
         * @param event 轨迹球事件。
         *
         * @return boolean 如果此事件被消耗则返回true。
         */
        public boolean dispatchTrackballEvent(MotionEvent event);

        /**
         * 调用以处理通用运动事件。至少您的实现必须调用
         * {@link android.view.Window#superDispatchGenericMotionEvent}来执行标准处理。
         *
         * @param event 通用运动事件。
         *
         * @return boolean 如果此事件被消耗则返回true。
         */
        public boolean dispatchGenericMotionEvent(MotionEvent event);

        /**
         * 调用以处理{@link AccessibilityEvent}的填充。
         *
         * @param event 事件。
         *
         * @return boolean 如果事件填充完成则返回true。
         */
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event);

        /**
         * 实例化要在'featureId'面板中显示的视图。
         * 您可以返回null，在这种情况下将为您创建默认内容（通常是菜单）。
         *
         * @param featureId 正在创建的面板。
         *
         * @return view 要放置在面板中的顶层视图。
         *
         * @see #onPreparePanel
         */
        @Nullable
        public View onCreatePanelView(int featureId);

        /**
         * 初始化面板'featureId'中菜单的内容。如果onCreatePanelView()返回null，
         * 这将为您提供一个标准菜单，您可以在其中放置项目。这只在面板第一次显示时调用一次。
         *
         * <p>您可以安全地保留<var>menu</var>（以及从中创建的任何项目），
         * 根据需要对其进行修改，直到下一次为该功能调用onCreatePanelMenu()。
         *
         * @param featureId 正在创建的面板。
         * @param menu 面板中的菜单。
         *
         * @return boolean 您必须返回true才能显示面板；如果返回false则不会显示。
         */
        boolean onCreatePanelMenu(int featureId, @NonNull Menu menu);

        /**
         * 准备要显示的面板。这在面板窗口每次显示之前都会被调用。
         *
         * @param featureId 正在显示的面板。
         * @param view 由onCreatePanelView()返回的视图。
         * @param menu 如果onCreatePanelView()返回null，这是在面板中显示的菜单。
         *
         * @return boolean 您必须返回true才能显示面板；如果返回false则不会显示。
         *
         * @see #onCreatePanelView
         */
        boolean onPreparePanel(int featureId, @Nullable View view, @NonNull Menu menu);

        /**
         * 当用户打开面板的菜单时调用。这也可能在菜单从一种类型更改为另一种类型时被调用
         * （例如，从图标菜单更改为扩展菜单）。
         *
         * @param featureId 菜单所在的面板。
         * @param menu 已打开的菜单。
         * @return 返回true允许菜单打开，或返回false阻止菜单打开。
         */
        boolean onMenuOpened(int featureId, @NonNull Menu menu);

        /**
         * 当用户选择面板的菜单项时调用。
         *
         * @param featureId 菜单所在的面板。
         * @param item 已选择的菜单项。
         *
         * @return boolean 返回true完成选择处理，或返回false执行正常的菜单处理
         * （调用其Runnable或向其目标Handler发送消息）。
         */
        boolean onMenuItemSelected(int featureId, @NonNull MenuItem item);

        /**
         * 当前窗口属性更改时调用。
         *
         */
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs);

        /**
         * 当屏幕的内容视图更改时调用（由于调用
         * {@link Window#setContentView(View, android.view.ViewGroup.LayoutParams)
         * Window.setContentView}或
         * {@link Window#addContentView(View, android.view.ViewGroup.LayoutParams)
         * Window.addContentView}）。
         */
        public void onContentChanged();

        /**
         * 当窗口焦点更改时调用。有关更多信息，请参见
         * {@link View#onWindowFocusChanged(boolean)
         * View.onWindowFocusChangedNotLocked(boolean)}。
         *
         * @param hasFocus 窗口是否现在有焦点。
         */
        public void onWindowFocusChanged(boolean hasFocus);

        /**
         * 当窗口附加到窗口管理器时调用。
         * 有关更多信息，请参见{@link View#onAttachedToWindow() View.onAttachedToWindow()}。
         */
        public void onAttachedToWindow();

        /**
         * 当窗口从窗口管理器分离时调用。
         * 有关更多信息，请参见{@link View#onDetachedFromWindow() View.onDetachedFromWindow()}。
         */
        public void onDetachedFromWindow();

        /**
         * 当面板正在关闭时调用。如果另一个逻辑后续面板正在打开
         * （并且此面板正在关闭以腾出空间给后续面板），则不会调用此方法。
         *
         * @param featureId 正在显示的面板。
         * @param menu 如果onCreatePanelView()返回null，这是在面板中显示的菜单。
         */
        void onPanelClosed(int featureId, @NonNull Menu menu);

        /**
         * 当用户表示希望开始搜索时调用。
         *
         * @return 如果搜索启动则返回true，如果活动拒绝（阻止）则返回false。
         *
         * @see android.app.Activity#onSearchRequested()
         */
        public boolean onSearchRequested();

        /**
         * 当用户表示希望开始搜索时调用。
         *
         * @param searchEvent 描述开始搜索信号的{@link SearchEvent}。
         * @return 如果搜索启动则返回true，如果活动拒绝（阻止）则返回false。
         */
        public boolean onSearchRequested(SearchEvent searchEvent);

        /**
         * 当为此窗口启动操作模式时调用。这给回调一个机会以自己独特而美丽的方式处理操作模式。
         * 如果此方法返回null，系统可以选择呈现模式的方式或选择不启动模式。
         * 这等同于{@link #onWindowStartingActionMode(android.view.ActionMode.Callback, int)}
         * 且类型为{@link ActionMode#TYPE_PRIMARY}。
         *
         * @param callback 控制此操作模式生命周期的回调
         * @return 已启动的操作模式，如果系统应该呈现它则返回null
         */
        @Nullable
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback);

        /**
         * 当为此窗口启动操作模式时调用。这给回调一个机会以自己独特而美丽的方式处理操作模式。
         * 如果此方法返回null，系统可以选择呈现模式的方式或选择不启动模式。
         *
         * @param callback 控制此操作模式生命周期的回调
         * @param type {@link ActionMode#TYPE_PRIMARY}或{@link ActionMode#TYPE_FLOATING}之一。
         * @return 已启动的操作模式，如果系统应该呈现它则返回null
         */
        @Nullable
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type);

        /**
         * 当操作模式已启动时调用。相应的模式回调方法将已被调用。
         *
         * @param mode 刚刚启动的新模式。
         */
        public void onActionModeStarted(ActionMode mode);

        /**
         * 当操作模式已完成时调用。相应的模式回调方法将已被调用。
         *
         * @param mode 刚刚完成的模式。
         */
        public void onActionModeFinished(ActionMode mode);

        /**
         * 当当前窗口需要键盘快捷方式时调用。
         *
         * @param data 要填充快捷方式的数据列表。
         * @param menu 当前菜单，可能为null。
         * @param deviceId 快捷方式应提供的连接设备的ID。
         */
        default public void onProvideKeyboardShortcuts(
                List<KeyboardShortcutGroup> data, @Nullable Menu menu, int deviceId) { };

        /**
         * 当当前窗口的指针捕获启用或禁用时调用。
         *
         * @param hasCapture 窗口是否具有指针捕获。
         */
        default public void onPointerCaptureChanged(boolean hasCapture) { };
    }

    /** @hide */
    public interface OnWindowDismissedCallback {
        /**
         * 当窗口被关闭时调用。这通知回调窗口已消失，它应该完成自身。
         * @param finishTask 如果任务也应该完成则为true。
         * @param suppressWindowTransition 如果结果退出和进入窗口转换动画应该被抑制则为true。
         */
        void onWindowDismissed(boolean finishTask, boolean suppressWindowTransition);
    }

    /** @hide */
    public interface OnWindowSwipeDismissedCallback {
        /**
         * 当窗口被滑动关闭时调用。这通知回调窗口已消失，它应该完成自身。
         * @param finishTask 如果任务也应该完成则为true。
         * @param suppressWindowTransition 如果结果退出和进入窗口转换动画应该被抑制则为true。
         */
        void onWindowSwipeDismissed();
    }

    /** @hide */
    public interface WindowControllerCallback {
        /**
         * 在{@link WindowConfiguration#WINDOWING_MODE_FREEFORM}窗口模式和
         * {@link WindowConfiguration#WINDOWING_MODE_FULLSCREEN}之间移动活动。
         */
        void toggleFreeformWindowingMode();

        /**
         * 如果活动支持，则将活动置于画中画模式。
         * @see android.R.attr#supportsPictureInPicture
         */
        void enterPictureInPictureModeIfPossible();

        /** 返回窗口是否属于任务根。 */
        boolean isTaskRoot();

        /**
         * 更新状态栏颜色为强制颜色。
         */
        void updateStatusBarColor(int color);

        /**
         * 更新状态栏外观。
         */

        void updateSystemBarsAppearance(int appearance);

        /**
         * 更新导航栏颜色为强制颜色。
         */
        void updateNavigationBarColor(int color);
    }

    /** @hide */
    public interface DecorCallback {
        /**
         * 从
         * {@link com.android.internal.policy.DecorView#onSystemBarAppearanceChanged(int)}调用。
         *
         * @param appearance 新应用的外观。
         */
        void onSystemBarAppearanceChanged(@WindowInsetsController.Appearance int appearance);

        /**
         * 从
         * {@link com.android.internal.policy.DecorView#updateColorViews(WindowInsets, boolean)}
         * 调用，当
         * {@link com.android.internal.policy.DecorView#mDrawLegacyNavigationBarBackground}正在更新时。
         *
         * @param drawLegacyNavigationBarBackground 正在设置到
         *        {@link com.android.internal.policy.DecorView#mDrawLegacyNavigationBarBackground}的新值。
         * @return 要设置到
         *   {@link com.android.internal.policy.DecorView#mDrawLegacyNavigationBarBackgroundHandled}
         *         的值，代表{@link com.android.internal.policy.DecorView}。
         *         {@code true}表示窗口可以代表
         *         {@link com.android.internal.policy.DecorView}渲染旧版导航栏背景。
         *         {@code false}表示让{@link com.android.internal.policy.DecorView}处理它。
         */
        boolean onDrawLegacyNavigationBarBackgroundChanged(
                boolean drawLegacyNavigationBarBackground);
    }

    /**
     * 希望了解标题绘制内容区域的客户端的回调。
     */
    public interface OnRestrictedCaptionAreaChangedListener {
        /**
         * 当标题绘制内容的区域更改时调用。
         *
         * @param rect 标题内容定位的区域，相对于顶层视图。
         */
        void onRestrictedCaptionAreaChanged(Rect rect);
    }

    /**
     * 希望获取窗口渲染的每一帧的帧时序信息的客户端的回调。
     */
    public interface OnFrameMetricsAvailableListener {
        /**
         * 当上一渲染帧的信息可用时调用。
         *
         * 如果此回调执行时间过长，报告可能会被丢弃，因为报告生产者无法等待消费者完成。
         *
         * 强烈建议客户端在此方法中通过
         * {@link FrameMetrics#FrameMetrics(FrameMetrics)}复制传入的FrameMetrics，
         * 并将额外的计算或存储推迟到另一个线程，以避免不必要地丢弃报告。
         *
         * @param window 帧显示的{@link Window}。
         * @param frameMetrics 可用的指标。此对象在每次调用时都会被重用，
         * 因此<strong>在此方法范围之外此引用无效</strong>。
         * @param dropCountSinceLastInvocation 自上次调用此回调以来丢弃的报告数量。
         */
        void onFrameMetricsAvailable(Window window, FrameMetrics frameMetrics,
                int dropCountSinceLastInvocation);
    }

    /**
     * 用于在内容上应用窗口插入的监听器。仅由框架使用以根据旧版SystemUI标志拟合内容。
     *
     * @hide
     */
    public interface OnContentApplyWindowInsetsListener {

        /**
         * 当窗口需要在其内容视图容器上应用插入时调用，这些插入通过调用{@link #setContentView}设置。
         * 该方法应确定在根级内容视图容器上应用哪些插入，以及应通过视图层次结构分发到内容视图的
         * {@link View#setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener)}的内容。
         *
         * @param view 要应用插入的视图。不得直接修改。
         * @param insets 即将分发的根级插入
         * @return 一个对，第一个元素包含要作为边距应用到根级内容视图的插入，
         * 第二个元素确定应分发到内容视图的内容。
         */
        @NonNull
        Pair<Insets, WindowInsets> onContentApplyWindowInsets(@NonNull View view,
                @NonNull WindowInsets insets);
    }


    public Window(@UiContext Context context) {
        mContext = context;
        mFeatures = mLocalFeatures = getDefaultFeatures(context);
    }

    /**
     * 返回此窗口策略运行的上下文，用于检索资源和其他信息。
     *
     * @return Context 提供给构造函数的上下文。
     */
    @UiContext
    public final Context getContext() {
        return mContext;
    }

    /**
     * 返回此窗口主题的{@link android.R.styleable#Window}属性。
     */
    public final TypedArray getWindowStyle() {
        synchronized (this) {
            if (mWindowStyle == null) {
                mWindowStyle = mContext.obtainStyledAttributes(
                        com.android.internal.R.styleable.Window);
            }
            return mWindowStyle;
        }
    }

    /**
     * 设置此窗口的容器。如果未设置，DecorWindow将作为顶级窗口运行；
     * 否则，它将与容器协商以适当显示自身。
     *
     * @param container 所需的包含窗口。
     */
    public void setContainer(Window container) {
        mContainer = container;
        if (container != null) {
            // 嵌入式屏幕从不具有标题。
            mFeatures |= 1<<FEATURE_NO_TITLE;
            mLocalFeatures |= 1<<FEATURE_NO_TITLE;
            container.mHasChildren = true;
        }
    }

    /**
     * 返回此窗口的容器。
     *
     * @return Window 包含窗口，如果这是顶级窗口则为null。
     */
    public final Window getContainer() {
        return mContainer;
    }

    public final boolean hasChildren() {
        return mHasChildren;
    }

    /** @hide */
    public final void destroy() {
        mDestroyed = true;
        onDestroy();
    }

    /** @hide */
    protected void onDestroy() {
    }

    /** @hide */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public final boolean isDestroyed() {
        return mDestroyed;
    }

    /**
     * 设置窗口管理器供此窗口使用，例如显示面板。这<em>不</em>用于显示窗口本身--必须由客户端完成。
     *
     * @param wm 添加新窗口的窗口管理器。
     */
    public void setWindowManager(WindowManager wm, IBinder appToken, String appName) {
        setWindowManager(wm, appToken, appName, false);
    }

    /**
     * 设置窗口管理器供此窗口使用，例如显示面板。这<em>不</em>用于显示窗口本身--必须由客户端完成。
     *
     * @param wm 添加新窗口的窗口管理器。
     */
    public void setWindowManager(WindowManager wm, IBinder appToken, String appName,
            boolean hardwareAccelerated) {
        mAppToken = appToken;
        mAppName = appName;
        mHardwareAccelerated = hardwareAccelerated;
        if (wm == null) {
            wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        }
        mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
    }

    void adjustLayoutParamsForSubWindow(WindowManager.LayoutParams wp) {
        CharSequence curTitle = wp.getTitle();
        if (wp.type >= WindowManager.LayoutParams.FIRST_SUB_WINDOW &&
                wp.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
            if (wp.token == null) {
                View decor = peekDecorView();
                if (decor != null) {
                    wp.token = decor.getWindowToken();
                }
            }
            if (curTitle == null || curTitle.length() == 0) {
                final StringBuilder title = new StringBuilder(32);
                if (wp.type == WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA) {
                    title.append("Media");
                } else if (wp.type == WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY) {
                    title.append("MediaOvr");
                } else if (wp.type == WindowManager.LayoutParams.TYPE_APPLICATION_PANEL) {
                    title.append("Panel");
                } else if (wp.type == WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL) {
                    title.append("SubPanel");
                } else if (wp.type == WindowManager.LayoutParams.TYPE_APPLICATION_ABOVE_SUB_PANEL) {
                    title.append("AboveSubPanel");
                } else if (wp.type == WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG) {
                    title.append("AtchDlg");
                } else {
                    title.append(wp.type);
                }
                if (mAppName != null) {
                    title.append(":").append(mAppName);
                }
                wp.setTitle(title);
            }
        } else if (wp.type >= WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW &&
                wp.type <= WindowManager.LayoutParams.LAST_SYSTEM_WINDOW) {
            // 我们不为此系统窗口设置应用程序令牌，因为生命周期应该是独立的。
            // 如果应用程序创建了一个系统窗口，然后应用程序进入停止状态，系统窗口不应受到影响
            // （仍可显示和接收输入事件）。
            if (curTitle == null || curTitle.length() == 0) {
                final StringBuilder title = new StringBuilder(32);
                title.append("Sys").append(wp.type);
                if (mAppName != null) {
                    title.append(":").append(mAppName);
                }
                wp.setTitle(title);
            }
        } else {
            if (wp.token == null) {
                wp.token = mContainer == null ? mAppToken : mContainer.mAppToken;
            }
