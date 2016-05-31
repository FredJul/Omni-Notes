package net.fred.taskgame.hero.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.activities.MainActivity;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.models.adapters.LevelSelectionAdapter;
import net.fred.taskgame.hero.models.adapters.RecyclerViewItemListener;

import butterknife.BindView;
import butterknife.ButterKnife;


public class LevelSelectionFragment extends Fragment {

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_level_selection, container, false);
        ButterKnife.bind(this, layout);

        LinearLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        mRecyclerView.setLayoutManager(layoutManager);


        LevelSelectionAdapter adapter = new LevelSelectionAdapter(Level.getAllLevelsList(), new RecyclerViewItemListener() {
            @Override
            public void onItemClicked(int position) {
                getMainActivity().switchToLevel(Level.getAllLevelsList().get(position));
            }
        });
        mRecyclerView.setAdapter(adapter);

        return layout;
    }

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}
