/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.afzaln.myweatherapp;

import android.content.Context;
import android.support.annotation.NonNull;

import com.afzaln.myweatherapp.data.source.WeatherRepository;
import com.afzaln.myweatherapp.data.source.local.WeatherLocalDataSource;
import com.afzaln.myweatherapp.data.source.remote.WeatherRemoteDataSource;

import com.afzaln.myweatherapp.data.source.location.LocationProvider;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enables injection of mock implementations for
 * {@link WeatherDataSource} at compile time. This is useful for testing, since it allows us to use
 * a fake instance of the class to isolate the dependencies and run a test hermetically.
 */
public class Injection {

    /**
     * There would be a mock weather repository here that would return mock things
     * but not today.
     *
     * @param context Application context
     * @return WeatherRepository
     */
    public static WeatherRepository provideWeatherRepository(@NonNull Context context) {
        checkNotNull(context);
        return WeatherRepository.getInstance(WeatherRemoteDataSource.getInstance(),
                WeatherLocalDataSource.getInstance(context));
    }

    /**
     * There would be a mock location provider here that would return mock locations
     * but not today.
     *
     * @param context Application context
     * @return LocationProvider
     */
    public static LocationProvider provideLocationProvider(@NonNull Context context) {
        checkNotNull(context);
        return LocationProvider.getInstance(context);
    }
}
