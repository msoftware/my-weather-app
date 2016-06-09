package com.afzaln.kijijiweather.weather;

import java.util.List;

import android.support.annotation.NonNull;

import com.afzaln.kijijiweather.data.Search;
import com.afzaln.kijijiweather.data.Weather;
import com.afzaln.kijijiweather.data.source.WeatherRepository;
import com.afzaln.kijijiweather.data.source.location.LocationProvider;
import com.afzaln.kijijiweather.weather.WeatherContract.Presenter;
import com.afzaln.kijijiweather.weather.WeatherContract.View;
import static com.google.common.base.Preconditions.checkNotNull;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by afzal on 2016-06-04.
 */
public class WeatherPresenter implements Presenter<WeatherContract.View> {
    private final String title;
    private final WeatherRepository weatherRepository;
    private WeatherContract.View weatherView;
    private final LocationProvider locationProvider;
    private CompositeSubscription subscriptions;

    private ViewState lastViewState;

    public WeatherPresenter(String title, @NonNull WeatherRepository weatherRepository, LocationProvider locationProvider) {
        this.title = title;
        this.weatherRepository = checkNotNull(weatherRepository);
        this.locationProvider = checkNotNull(locationProvider);
        subscriptions = new CompositeSubscription();
    }

    @Override
    public void onViewAttached(View view) {
        onViewAttached(view, true);
    }

    @Override
    public void onViewAttached(View view, boolean load) {
        weatherView = view;
        if (load) {
            if (lastViewState != null) {
                updateView(lastViewState, false);
            }
            doLastWeatherSearch();
        }
    }

    public void onViewDetached() {
        weatherView = null;
        subscriptions.clear();
    }

    public void onDestroyed() {
        // nothing to clean up
    }

    @Override
    public void doCoordinatesWeatherSearch() {
        Observable<ViewState> weatherObservable = locationProvider.getLastLocation()
                // to obtain location in the background thread
                .observeOn(Schedulers.io())
                .map(location -> {
                    Timber.d("Observable gotLocation: " + Thread.currentThread().getName());
                    if (location != null) {
                        Search search = new Search();
                        search.setLatLon(location.getLatitude(), location.getLongitude());
                        return search;
                    } else {
                        // TODO maybe show error?
                        return null;
                    }
                })
                .compose(loadWeather(true))
                .compose(updateSearches())
                .compose(applySchedulers());

        updateView(weatherObservable);
    }

    @Override
    public void doStringWeatherSeach(String searchStr) {
        if (searchStr == null || searchStr.isEmpty()) {
            // don't do anything if the EditText is empty
        } else {
            Search search = new Search();
            if (Search.determineSearchType(searchStr) == Search.SEARCH_TYPE_ZIPCODE) {
                search.setZipCode(searchStr);
            } else {
                search.setSearchStr(searchStr);
            }
            Observable<ViewState> stateObservable = Observable.just(search)
                    .compose(loadWeather(true))
                    .compose(updateSearches())
                    .compose(applySchedulers());

            updateView(stateObservable);
        }
    }

    public void doLastWeatherSearch() {
        Observable<ViewState> stateObservable = weatherRepository.getRecentSearches()
                .map(searches -> {
                    if (!searches.isEmpty()) {
                        return searches.get(0);
                    } else {
                        return null;
                    }
                })
                .compose(loadWeather(true))
                .compose(updateSearches())
                .compose(applySchedulers());

        updateView(stateObservable);
    }

    private Transformer<Weather, ViewState> updateSearches() {
        return weatherObservable -> weatherObservable
                .flatMap(weather -> weatherRepository.getRecentSearches()
                        .map(searches -> new ViewState(weather, searches)));
    }

    Transformer<Search, Weather> loadWeather(boolean forceUpdate) {
        return searchObservable -> searchObservable
                .flatMap(search -> {
                    Timber.d("Weather search: " + Thread.currentThread().getName());
                    if (search == null) {
                        return null;
                    }
                    if (forceUpdate) {
                        // forces the repository to skip the cache
                        weatherRepository.refreshWeather();
                    }
                    return weatherRepository.getWeather(search);
                });
    }

    private void updateView(ViewState viewState, boolean animate) {
        updateViewSearches(viewState.searches);
        if (viewState.weather == null) {
            showEmptyWeather();
        } else {
            updateViewWeather(viewState.weather, animate);
        }
    }

    public static <T> Transformer<T, T> applySchedulers() {
        return observable -> observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Subscription updateView(Observable<ViewState> observable) {
        return observable
                .subscribe(viewState -> {
                    // onNext
                    updateView(viewState, true);
                    lastViewState = viewState;
                }, throwable -> {
                    // onError
                    showError(throwable);
                }, () -> {
                    // onCompleted
                    weatherView.setLoadingIndicator(false);
                });
    }

    @Override
    public void deleteRecentSearch(@NonNull Search search) {
        if (search != null) {
            weatherRepository.deleteRecentSearch(search.getTimestamp());
            weatherRepository.getRecentSearches()
                    .<List<Search>>map(searches -> searches)
                    .compose(applySchedulers())
                    .subscribe(searches -> {
                        updateViewSearches(searches);
                    });
        }
    }

    private void updateViewSearches(List<Search> searches) {
        if (weatherView != null) {
            weatherView.populateRecentSearches(searches);
        }
    }

    private void showEmptyWeather() {
        if (weatherView != null) {
            weatherView.showEmptyWeather();
            weatherView.setLoadingIndicator(false);
        }
    }

    private void updateViewWeather(Weather weather, boolean animate) {
        if (weatherView != null) {
            weatherView.showWeather(weather, animate);
            weatherView.setLoadingIndicator(false);
        }
    }

    private void showError(Throwable throwable) {
        if (weatherView != null) {
            weatherView.showError(throwable.getMessage());
            weatherView.setLoadingIndicator(false);
        }
    }

    public static class ViewState {
        List<Search> searches;
        Weather weather;

        public ViewState(Weather weather, List<Search> searches) {
            this.weather = weather;
            this.searches = searches;
        }
    }
}
