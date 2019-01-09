package cgeo.geocaching.ui;

import cgeo.geocaching.activity.AbstractViewPagerActivity.PageViewCreator;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * View creator which destroys the created view on every {@link #notifyDataSetChanged()}.
 *
 */
public abstract class AbstractCachingPageViewCreator<ViewClass extends View> implements PageViewCreator {

    public ViewClass view;

    @Override
    public final void notifyDataSetChanged() {
        view = null;
    }

    @Override
    public final View getView(final ViewGroup parentView) {
        if (view == null) {
            view = getDispatchedView(parentView);
        }

        return view;
    }

    @Override
    @SuppressFBWarnings("USM_USELESS_ABSTRACT_METHOD")
    public abstract ViewClass getDispatchedView(ViewGroup parentView);

    /**
     * Gets the state of the view but returns an empty state if not overridden
     *
     * @return empty bundle
     */
    @Nullable
    @Override
    public Bundle getViewState() {
        return new Bundle();
    }

    /**
     * Restores the state of the view but just returns if not overridden.
     */
    @Override
    public void setViewState(@NonNull final Bundle state) {
    }
}
