package com.example.wilbotiv.popularmoviess1.fragments;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wilbotiv.popularmoviess1.fetches.FetchReviewTask;
import com.example.wilbotiv.popularmoviess1.fetches.FetchTrailerTask;
import com.example.wilbotiv.popularmoviess1.db.MovieContract;
import com.example.wilbotiv.popularmoviess1.R;
import com.squareup.picasso.Picasso;

// DONE: 10/11/2016 You are performing database operations (which are POTENTIALLY lengthy operations) on the main UI thread (You can learn more about threads from the Udacity courses: Main Thread vs Background Thread). This could cause a stutter or an ANR (which is short for Application Not Responding, and usually occurs when the main thread of the application is blocked for too long). You can either put this type of operations inside of an AsyncTask or use an AsyncQueryHandler (for easier asynchronous access to Content Providers. Here is a simple tutorial that you can get started with.). Also it's better if you could show a progress indicator to the user. You can learn more about how to make your app responsive here from the Official Android Developer Website.
// DONE: 10/11/2016 Please remove the commented out code.
// TODO: 10/13/2016 give user ability to un-favorite
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DETAIL_URI = "URI";
    private Uri mUri;
    private String mMovieID;
    private String mSource;

    private static final int DETAIL_LOADER_HEADER = 0;
    private static final int DETAIL_LOADER_REVIEWS = 1;
    private static final int DETAIL_LOADER_TRAILERS = 2;


    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    private static final String MOVIE_SHARE_HASHTAG = " #PopularMoviesS1";
    private ShareActionProvider mShareActionProvider;
    //FIXED: mMovie needs to have originalTitle
    private String mMovie;
    private final String INTENT_KEY = "com.example.wilbotiv.popularmoviess1.movieID";

    String posterPath;
    String originalTitle;
    String releaseDate;
    String overview;
    String movieID;
    String voteAverage;


    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry.COLUMN_POSTERPATH,
            MovieContract.MovieEntry.COLUMN_ORIGINALTITLE,
            MovieContract.MovieEntry.COLUMN_RELEASEDATE,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_VOTEAVERAGE,
    };

    private static final int COL_POSTER_PATH = 0;
    private static final int COL_ORIGINAL_TITLE = 1;
    private static final int COL_REALEASEDATE = 2;
    private static final int COL_OVERVIEW = 3;
    private static final int COL_MOVIE_ID = 4;
    private static final int COL_VOTE_AVERAAGE = 5;

//// DONE: 8/28/2016 Projection and index for case 1

    private static final String[] REVIEWS_COLUMNS = {
            MovieContract.ReviewEntry.COLUMN_AUTHOR,
            MovieContract.ReviewEntry.COLUMN_CONTENT
    };

    private static final int COL_AUTHOR = 0;
    private static final int COL_CONTENT = 1;

    private static final String[] TRAILER_COLUMNS = {
            MovieContract.TrailerEntry.COLUMN_NAME,
            MovieContract.TrailerEntry.COLUMN_SOURCE
    };

    private static final int COL_NAME = 0;
    private static final int COL_SOURCE = 1;


    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(DETAIL_LOADER_HEADER, null, this);
        getLoaderManager().initLoader(DETAIL_LOADER_REVIEWS, null, this);
        getLoaderManager().initLoader(DETAIL_LOADER_TRAILERS, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
            mMovieID = mUri.getLastPathSegment();
        }


        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        Button button = (Button) rootView.findViewById(R.id.fragment_detail_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                ContentValues movieValues = new ContentValues();

                movieValues.put(MovieContract.FavoriteEntry.COLUMN_POSTERPATH, posterPath);
                movieValues.put(MovieContract.FavoriteEntry.COLUMN_ORIGINALTITLE, originalTitle);
                movieValues.put(MovieContract.FavoriteEntry.COLUMN_RELEASEDATE, releaseDate);
                movieValues.put(MovieContract.FavoriteEntry.COLUMN_OVERVIEW, overview);
                movieValues.put(MovieContract.FavoriteEntry.COLUMN_MOVIE_ID, movieID);
                movieValues.put(MovieContract.FavoriteEntry.COLUMN_VOTEAVERAGE, voteAverage);

                ContentResolver resolver = getContext().getContentResolver();

                AsyncQueryHandler queryHandler = new AsyncQueryHandler(resolver) {
                    @Override
                    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                        super.onQueryComplete(token, cookie, cursor);

                        Toast toast = Toast.makeText(getContext(), "'" + originalTitle + "' has been added to your Favorites", Toast.LENGTH_SHORT);
                        toast.show();

                    }

                };
                queryHandler.startInsert(0, null, MovieContract.FavoriteEntry.CONTENT_URI, movieValues);
            }
        });
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mMovie != null) {
            mShareActionProvider.setShareIntent(createShareMovieIntent());
        }
    }

    //FIXED: Share intent does not work
    private Intent createShareMovieIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        //FIXED: Add Original Title here.
        shareIntent.putExtra(Intent.EXTRA_TEXT, "http://www.youtube.com/watch?v=" + mSource);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//            DONE:Init another loader here? - No.
        Log.v(LOG_TAG, "In onActivityCreated");
        updateMovieReview();
        updateMovieTrailer();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        int loaderID = id;

        switch (loaderID) {
            case 0:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String sortOrder = sharedPreferences.getString("sort_order", "0");
                int sortOrderValueToInt = Integer.parseInt(sortOrder);
                Uri loaderUri;
                if (sortOrderValueToInt == 0 || sortOrderValueToInt == 1) {
                    loaderUri = MovieContract.MovieEntry.CONTENT_URI;
                } else {
                    loaderUri = MovieContract.FavoriteEntry.CONTENT_URI;
                }
                Log.v(LOG_TAG, "In onCreateLoader case 0");
                return new CursorLoader(
                        getActivity(),
                        loaderUri,
                        MOVIE_COLUMNS,
                        MovieContract.MovieEntry.COLUMN_MOVIE_ID + " =" + mMovieID,
                        null,
                        null
                );

            case 1:
//                    DONE: You cant use movieID, because it may be null. Get movieID from intent?
                Log.v(LOG_TAG, "In onCreateLoader case 1");
                return new CursorLoader(
                        getActivity(),
                        MovieContract.ReviewEntry.CONTENT_URI,
                        REVIEWS_COLUMNS,
                        MovieContract.ReviewEntry.COLUMN_MOVIE_ID + " =" + mMovieID,
                        null,
                        null
                );

            case 2:
//                    DONE: You cant use movieID, because it may be null. Get movieID from intent?
                Log.v(LOG_TAG, "In onCreateLoader case 2");
                return new CursorLoader(
                        getActivity(),
                        MovieContract.TrailerEntry.CONTENT_URI,
                        TRAILER_COLUMNS,
                        MovieContract.TrailerEntry.COLUMN_MOVIE_ID + " =" + mMovieID,
                        null,
                        null
                );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int loaderID = loader.getId();
        Log.v(LOG_TAG, "In onLoadFinished ");
        Context context = getContext();

        if (!data.moveToFirst()) {
            return;
        }
//DONE: maybe make these member variables......

        switch (loaderID) {
            case 0:
                Log.v(LOG_TAG, "In onLoadFinished case 0");

                posterPath = data.getString(COL_POSTER_PATH);
                originalTitle = data.getString(COL_ORIGINAL_TITLE);
                releaseDate = data.getString(COL_REALEASEDATE);
                overview = data.getString(COL_OVERVIEW);
                movieID = data.getString(COL_MOVIE_ID);
                voteAverage = data.getString(COL_VOTE_AVERAAGE);

                mMovie = originalTitle;

                TextView textViewOriginalTitle = (TextView) getView().findViewById(R.id.fragment_detail_textView_originalTitle);
                ImageView imageView = (ImageView) getView().findViewById(R.id.fragment_detail_imageView_poster);
                TextView textViewReleaseDate = (TextView) getView().findViewById(R.id.fragment_detail_textView_releaseDate);
                TextView textViewVoteAverage = (TextView) getView().findViewById(R.id.fragment_detail_textView_voteAverage);
                TextView textViewMovieID = (TextView) getView().findViewById(R.id.fragment_detail_textView_movieID);
                TextView textViewOverview = (TextView) getView().findViewById(R.id.fragment_detail_textView_overview);
//
                textViewOriginalTitle.setText(originalTitle);
                textViewReleaseDate.setText(releaseDate);
                textViewVoteAverage.setText(voteAverage);
                textViewMovieID.setText(movieID);
                textViewOverview.setText(overview);

                Picasso.with(context).load("http://image.tmdb.org/t/p/w500" + posterPath).into(imageView);
//DONE: updateMoviewReview() causes onLoadFinished() to run multiple times.
                break;

            case 1:
                Log.v(LOG_TAG, "In onLoadFinished case 1");
//                    DONE: Create some views.
//                    DONE: Change index in cursor to FINAL variable

//                    Using removeAllViews because onLoadFinished is being called twice even though I have
//                    it init'ing cursorLoader in onResume(). I think because my fetchMovieReview (AsyncTask) is not
//                    completed when onLoadFinish runs query??? Anyway this seems to work.

//                    DONE: This might be a problem with trailers...
                LinearLayout linearLayout = (LinearLayout) getView().findViewById(R.id.fragment_detail_linearLayout_review);
                linearLayout.removeAllViews();

                for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
                    String reviewerName = data.getString(COL_AUTHOR);
                    String content = data.getString(COL_CONTENT);
                    Log.v(LOG_TAG, " " + reviewerName + " " + content);
//DONE: use member variable context
                    TextView textViewReviewerName = new TextView(context);
                    TextView textViewContent = new TextView(context);

                    textViewReviewerName.setText(reviewerName);
//                        DONE: Use r.demension instead of hard code
                    textViewReviewerName.setTextSize(getResources().getDimension(R.dimen.text_size_reviewer));
                    textViewReviewerName.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));

                    textViewContent.setText(content);
                    textViewContent.setTextSize(getResources().getDimension(R.dimen.text_size_review));
                    textViewContent.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));

                    linearLayout.addView(textViewReviewerName);
                    linearLayout.addView(textViewContent);
//                      DONE: I don't need count anymore, right?
//DONE: Details page listing reviews twice....
                }
                break;

            case 2:
                Log.v(LOG_TAG, "In onLoadFinished case 2");
                LinearLayout linearLayoutTrailer = (LinearLayout) getView().findViewById(R.id.fragment_detail_linearLayout_trailer);
                linearLayoutTrailer.removeAllViews();

                for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
                    final String name = data.getString(COL_NAME);
                    mSource = data.getString(COL_SOURCE);

                    Log.v(LOG_TAG, "In onLoadFinished case 2 " + name + " " + mSource);

                    Button trailerButton = new Button(getContext());
// DONE: 8/26/2016 use string resource

                    trailerButton.setText(name);
                    trailerButton.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));
                    trailerButton.setOnClickListener(new View.OnClickListener() {

                                                         @Override
                                                         public void onClick(View v) {
                                                             Toast.makeText(getContext(), name, Toast.LENGTH_SHORT).show();
                                                             Log.v(LOG_TAG, "In on click");

                                                             Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + mSource));

                                                             if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                                                                 startActivity(intent);
                                                             }
                                                         }
                                                     }
                    );
                    linearLayoutTrailer.addView(trailerButton);
                }
                break;
        }
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareMovieIntent());
        }
    }

    private void updateMovieReview() {
        FetchReviewTask fetchReviewTask = new FetchReviewTask(getContext());
        Log.v(LOG_TAG, "In updateMovieReview");
        fetchReviewTask.execute(mMovieID);
    }

    private void updateMovieTrailer() {
        FetchTrailerTask fetchTrailerTask = new FetchTrailerTask(getContext());
        Log.v(LOG_TAG, "In updateMovieTrailer");
        fetchTrailerTask.execute(mMovieID);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}