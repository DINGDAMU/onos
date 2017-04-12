package org.onosproject.millimeterwavegui;

import org.apache.commons.math3.special.Erf;

public final class Psuccess {

    public  double getPs(double d) {
        double pl = 0.0;
        double alpha = 0.0;  //dB
        double beta = 0.0;
        double sigma = 0.0;   //dB
        double sigmaLin = 0.0;
        double xsiLin = 0.0;
        double xsi = 0.0;
        double fc;
        String plModel = "28";
        if (plModel.equals("28")) {
            alpha = 72.0;
            beta = 2.92;
            sigma = 8.7;
            sigmaLin = Math.pow(10, sigma / 10);
            xsiLin = Math.pow(sigmaLin, 2);
            xsi = 10 * Math.log10(xsiLin);
            pl = alpha + 10 * beta * Math.log10(d);
        } else if (plModel.equals("73")) {
            alpha = 86.6;
            beta = 2.45;
            sigma = 8.0;
            sigmaLin = Math.pow(10, sigma / 10);
            xsiLin = Math.pow(sigmaLin, 2);
            xsi = 10 * Math.log10(xsiLin);
            pl = alpha + 10 * beta * Math.log10(d);

        } else if (plModel.equals("3gpp")) {
            fc = 2.5;
            pl = 22.7 + 36.7 * Math.log10(d) + 26 * Math.log10(fc);
            beta = 3.67;
            alpha = 70.0;
        } else {
            System.out.println("Please type the right pl_model");
        }

        double lamda = 100.0; //density of mm-wave links
        double b = 2e9;         //mm-wave bandwidth
        double c = 0.11;      //fractional Los area in the model developed


        double gmax = 18.0;  //dB
        double gmaxLin = Math.pow(10, gmax / 10);

        double pb = 30.0;   //dB
        double pbLin = Math.pow(10, pb / 10);

        double pn = -174.0 + 10 * Math.log10(b) + 10;
        double pnLin = Math.pow(10, pn / 10);

        double plLin = Math.pow(10, pl / 10);

        double snr0 = pb + gmax - pn;
        double snr0Lin = Math.pow(10, snr0 / 10);

        double snr = snr0 - pl;
        double snrLin = Math.pow(10, snr / 10);
        double xsiLLin = 5.2;        //xsi corresponding path-loss standard deviation
        double xsiNLin = 7.6;

        //beta_l_n = one meter loss #dB
        double betaLN = alpha; //db
        double betaLNLin = Math.pow(10, betaLN / 10);

        //ml_db = -0.1 *beta_l_lin *log(10)
        double ml = -Math.log(betaLNLin);
        double sigmaL = 0.1 * xsiLLin * Math.log(10);

        //mn_db = -0.1*beta_n_lin * log(10)
        double mn = -Math.log(betaLNLin);
        double sigmaN = 0.1 * xsiNLin * Math.log(10);

        double tau = 3.0;
        double tauLin = Math.pow(10, tau / 10);


        //((Pb_lin*Gmax_lin)/(pl_lin*pn_lin)
        double factor = snrLin / tauLin;

        double pc1Q1 = qfun((Math.log((Math.pow(d, beta) / factor)) - ml) / sigmaL);
        double pc1Q2 = qfun((Math.log((Math.pow(d, pl) / factor)) - mn) / sigmaN);
        double pc1 = Math.pow(d, 2) * (pc1Q1 - pc1Q2);
        double pc2 = pcC1(sigmaL, ml, beta, factor) * pcC2(sigmaL, ml, d, beta, factor);
        double pc3 = pcC1(sigmaN, mn, beta, factor) * (1 / c - pcC2(sigmaN, mn, d, beta, factor));
        double pc = pc1 + pc2 + pc3;
        double lamdaA = lamda * Math.PI * c * pc;
        double ma = lamdaA / lamda;
        double ps = 1 - Math.exp(-lamda * ma * factor);
        return ps;
    }

    private  double qfun(double x) {
        return 0.5 * Erf.erfc(x / Math.sqrt(2));
    }

    private  double pcC1(double sigma, double m, double beta, double factor) {
        return Math.pow(factor, 2 / beta) * Math.exp(2 * ((sigma * sigma) / (beta * beta)) + 2 * (m / beta));
    }

    private   double pcC2(double sigma, double m, double d, double beta, double factor) {
        return qfun((sigma * sigma * (2 / beta) - Math.log(Math.pow(d, beta / factor)) + m) / sigma);

    }





}
