data {
    int<lower=1> nobs;          # Number of obs per species.
    int<lower=1> norder;        # Number of orders.
    int<lower=1> nfamily;       # Number of families.
    int<lower=1> ngenus;        # Number of genera.

    int y[nobs];
    int order[nobs];
    int family[nobs];
    int genus[nobs];
    int years_in_list[nobs];
}

parameters {
    real lambda_0;
    vector[norder] pi_iR;
    vector[nfamily] tau_jR;
    vector[ngenus] rho_kR;
    real<lower=0> sigma_i;
    real<lower=0> sigma_j;
    real<lower=0> sigma_k;
}

transformed parameters {
    vector[norder] pi_i;
    vector[nfamily] tau_j;
    vector[ngenus] rho_k;

    pi_i = pi_iR * sigma_i;
    tau_j = tau_jR * sigma_j;
    rho_k = rho_kR * sigma_k;
}

model {
    lambda_0 ~ normal(0, 1);
    sigma_i ~ normal(0, 1);
    sigma_j ~ normal(0, 1);
    sigma_k ~ normal(0, 1);

    pi_iR ~ normal(0, 1);
    tau_jR ~ normal(0, 1);
    rho_kR ~ normal(0, 1);

    for(i in 1:nobs)
        y[i] ~ poisson(exp(lambda_0 + pi_i[order[i]] + tau_j[family[i]] + rho_k[genus[i]] + log(years_in_list[i])));
}
