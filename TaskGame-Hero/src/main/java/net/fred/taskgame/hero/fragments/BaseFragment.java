package net.fred.taskgame.hero.fragments;

import android.support.annotation.RawRes;
import android.support.v4.app.Fragment;

import net.fred.taskgame.hero.activities.MainActivity;


public abstract class BaseFragment extends Fragment {

    @Override
    public void onStart() {
        super.onStart();

        getMainActivity().playMusic(getMainMusicResId());
    }

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    protected abstract
    @RawRes
    int getMainMusicResId();
}
