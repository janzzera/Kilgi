package com.example.kilgi.inventory.network;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.common.AppExecutors;
import com.example.kilgi.inventory.data.common.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class NetworkBoundResource<ResultType, RequestType> {
    private final MediatorLiveData<Resource<ResultType>> result = new MediatorLiveData<>();

    public NetworkBoundResource() {
        result.setValue(Resource.loading(null));

        LiveData<ResultType> dbSource = loadFromDb();

        result.addSource(dbSource, data -> {
            result.removeSource(dbSource);

            if(shouldFetch(data)) {
                fetchFromNetwork(dbSource);
            } else {
                result.addSource(dbSource, newData ->
                        result.setValue(Resource.success(newData)));
            }
        });
    }

    private void fetchFromNetwork(LiveData<ResultType> dbSource) {
        result.addSource(dbSource, oldData ->
                result.setValue(Resource.loading(oldData)));

        createCall().enqueue(new Callback<RequestType>() {
            @Override
            public void onResponse(Call<RequestType> call, Response<RequestType> response) {
                result.removeSource(dbSource);

                if (response.isSuccessful() && response.body() != null) {
                    KilgiDatabase.databaseWriteExecutor.execute(() -> {
                        saveCallResult(response.body());

                        AppExecutors.runOnMainThread(() -> {
                            result.addSource(loadFromDb(), freshData ->
                                    result.setValue(Resource.success(freshData)));
                        });
                    });
                } else {
                    result.addSource(dbSource, oldData ->
                            result.setValue(Resource.error("HTTP Error: " + response.code(), oldData)));
                }
            }

            @Override
            public void onFailure(Call<RequestType> call, Throwable throwable) {
                result.removeSource(dbSource);
                result.addSource(dbSource, oldData ->
                        result.setValue(Resource.error(throwable.getLocalizedMessage(), oldData)));
            }
        });
    }

    public LiveData<Resource<ResultType>> asLiveData() {
        return result;
    }

    //child class defines these specific to the data model
    protected abstract LiveData<ResultType> loadFromDb();
    protected abstract boolean shouldFetch(ResultType data);
    protected abstract Call<RequestType> createCall();
    protected abstract void saveCallResult(RequestType item);
}
