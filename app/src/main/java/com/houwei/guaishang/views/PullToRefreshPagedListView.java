package com.houwei.guaishang.views;

import com.houwei.guaishang.R;

import android.R.color;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.ListAdapter;


public class PullToRefreshPagedListView  extends PullToRefreshAdapterViewBase<PagedListView> {

	private PullToRefreshLoadingLayout mHeaderLoadingView;
	private PullToRefreshLoadingLayout mFooterLoadingView;

	private FrameLayout mLvFooterLoadingFrame;
	private boolean mAddedLvFooter = false;

	class InternalListView extends PagedListView implements PullToRefreshEmptyViewMethodAccessor {

		public InternalListView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		public void setAdapter(ListAdapter adapter) {
			// Add the Footer View at the last possible moment
			if (!mAddedLvFooter && null != mLvFooterLoadingFrame) {
				addFooterView(mLvFooterLoadingFrame, null, true);
				mAddedLvFooter = true;
			}

			super.setAdapter(adapter);
		}

		@Override
		public void setEmptyView(View emptyView) {
			PullToRefreshPagedListView.this.setEmptyView(emptyView);
		}

		@Override
		public void setEmptyViewInternal(View emptyView) {
			super.setEmptyView(emptyView);
		}

		public ContextMenuInfo getContextMenuInfo() {
			return super.getContextMenuInfo();
		}
	}

	public PullToRefreshPagedListView(Context context) {
		super(context);
		setDisableScrollingWhileRefreshing(false);
	}

	public PullToRefreshPagedListView(Context context, int mode) {
		super(context, mode);
		setDisableScrollingWhileRefreshing(false);
	}

	public PullToRefreshPagedListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDisableScrollingWhileRefreshing(false);
	}

	@Override
	public ContextMenuInfo getContextMenuInfo() {
		return ((InternalListView) getRefreshableView()).getContextMenuInfo();
	}

	public void setReleaseLabel(String releaseLabel) {
		super.setReleaseLabel(releaseLabel);

		if (null != mHeaderLoadingView) {
			mHeaderLoadingView.setReleaseLabel(releaseLabel);
		}
		if (null != mFooterLoadingView) {
			mFooterLoadingView.setReleaseLabel(releaseLabel);
		}
	}

	public void setPullLabel(String pullLabel) {
		super.setPullLabel(pullLabel);

		if (null != mHeaderLoadingView) {
			mHeaderLoadingView.setPullLabel(pullLabel);
		}
		if (null != mFooterLoadingView) {
			mFooterLoadingView.setPullLabel(pullLabel);
		}
	}

	public void setRefreshingLabel(String refreshingLabel) {
		super.setRefreshingLabel(refreshingLabel);

		if (null != mHeaderLoadingView) {
			mHeaderLoadingView.setRefreshingLabel(refreshingLabel);
		}
		if (null != mFooterLoadingView) {
			mFooterLoadingView.setRefreshingLabel(refreshingLabel);
		}
	}

	@Override
	protected final PagedListView createRefreshableView(Context context, AttributeSet attrs) {
		PagedListView lv = new InternalListView(context, attrs);
		final int mode = getMode();

		// Loading View Strings
		String pullLabel = context.getString(R.string.pull_to_refresh_pull_label);
		String refreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
		String releaseLabel = context.getString(R.string.pull_to_refresh_pull_label);

		// Get Styles from attrs
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppStorePullToRefresh);

		// Add Loading Views
		if (mode == MODE_PULL_DOWN_TO_REFRESH || mode == MODE_BOTH) {
			FrameLayout frame = new FrameLayout(context);
			mHeaderLoadingView = new PullToRefreshLoadingLayout(context, MODE_PULL_DOWN_TO_REFRESH, releaseLabel, null,
					refreshingLabel, a,0);
			frame.addView(mHeaderLoadingView, FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT);
			mHeaderLoadingView.setVisibility(View.GONE);
			lv.addHeaderView(frame, null, true);
		}
		if (mode == MODE_PULL_UP_TO_REFRESH || mode == MODE_BOTH) {
			mLvFooterLoadingFrame = new FrameLayout(context);
			mFooterLoadingView = new PullToRefreshLoadingLayout(context, MODE_PULL_UP_TO_REFRESH, releaseLabel, pullLabel,
					refreshingLabel, a,1);
			mLvFooterLoadingFrame.addView(mFooterLoadingView, FrameLayout.LayoutParams.FILL_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT);
			mFooterLoadingView.setVisibility(View.GONE);
		}

		a.recycle();

		// Set it to this so it can be used in ListActivity/ListFragment
		lv.setId(android.R.id.list);
		return lv;
	}

	@Override
	protected void setRefreshingInternal(boolean doScroll) {

		// If we're not showing the Refreshing view, or the list is empty, then
		// the header/footer views won't show so we use the
		// normal method
		ListAdapter adapter = mRefreshableView.getAdapter();
		if (!getShowViewWhileRefreshing() || null == adapter || adapter.isEmpty()) {
			super.setRefreshingInternal(doScroll);
			return;
		}

		super.setRefreshingInternal(false);

		final PullToRefreshLoadingLayout originalLoadingLayout, listViewLoadingLayout;
		final int selection, scrollToY;

		switch (getCurrentMode()) {
			case MODE_PULL_UP_TO_REFRESH:
				originalLoadingLayout = getFooterLayout();
				listViewLoadingLayout = mFooterLoadingView;
				selection = mRefreshableView.getCount() - 1;
				scrollToY = getScrollY() - getHeaderHeight();
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				originalLoadingLayout = getHeaderLayout();
				listViewLoadingLayout = mHeaderLoadingView;
				selection = 0;
				scrollToY = getScrollY() + getHeaderHeight();
				break;
		}

		if (doScroll) {
			// We scroll slightly so that the ListView's header/footer is at the
			// same Y position as our normal header/footer
			setHeaderScroll(scrollToY);
		}

		// Hide our original Loading View
		originalLoadingLayout.setVisibility(View.INVISIBLE);

		// Show the ListView Loading View and set it to refresh
		listViewLoadingLayout.setVisibility(View.VISIBLE);
		listViewLoadingLayout.refreshing();

		if (doScroll) {
			// Make sure the ListView is scrolled to show the loading
			// header/footer
			mRefreshableView.setSelection(selection);

			// Smooth scroll as normal
			smoothScrollTo(0);
		}
	}

	@Override
	protected void resetHeader() {

		// If we're not showing the Refreshing view, or the list is empty, then
		// the header/footer views won't show so we use the
		// normal method
		ListAdapter adapter = mRefreshableView.getAdapter();
		if (!getShowViewWhileRefreshing() || null == adapter || adapter.isEmpty()) {
			super.resetHeader();
			return;
		}

		PullToRefreshLoadingLayout originalLoadingLayout;
		PullToRefreshLoadingLayout listViewLoadingLayout;

		int scrollToHeight = getHeaderHeight();
		int selection;

		switch (getCurrentMode()) {
			case MODE_PULL_UP_TO_REFRESH:
				originalLoadingLayout = getFooterLayout();
				listViewLoadingLayout = mFooterLoadingView;

				selection = mRefreshableView.getCount() - 1;
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				originalLoadingLayout = getHeaderLayout();
				listViewLoadingLayout = mHeaderLoadingView;
				scrollToHeight *= -1;
				selection = 0;
				break;
		}

		// Set our Original View to Visible
		originalLoadingLayout.setVisibility(View.VISIBLE);

		// Scroll so our View is at the same Y as the ListView header/footer,
		// but only scroll if we've pulled to refresh
		if (getState() != MANUAL_REFRESHING) {
			mRefreshableView.setSelection(selection);
			setHeaderScroll(scrollToHeight);
		}

		// Hide the ListView Header/Footer
		listViewLoadingLayout.setVisibility(View.GONE);

		super.resetHeader();
	}

	protected int getNumberInternalHeaderViews() {
		return null != mHeaderLoadingView ? 1 : 0;
	}

	protected int getNumberInternalFooterViews() {
		return null != mFooterLoadingView ? 1 : 0;
	}


	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if(onPlvScrollListener != null) {
			onPlvScrollListener.onScrollChanged(l, t, oldl, oldt);
		}
	}
	
	private OnPlvScrollListener onPlvScrollListener;
	
	public void setOnPlvScrollListener(OnPlvScrollListener onPlvScrollListener) {
		this.onPlvScrollListener = onPlvScrollListener;
	}

	public interface OnPlvScrollListener {
		void onScrollChanged(int l, int t, int oldl, int oldt);
	}
	
}