package com.example.kilgi.inventory.data.api;

import com.example.kilgi.inventory.data.AccountingPeriodEntity;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    @GET("accountingperiods")
    Call<List<AccountingPeriodEntity>> getAccountingPeriods();
}
