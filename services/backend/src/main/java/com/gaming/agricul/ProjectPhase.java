package com.gaming.agricul;

/**
 * Generic lifecycle phase used to look up the risk factor for the Unités de Contribution
 * (UC) calculation. Kept generic (not poultry-specific like the document's illustrative
 * "pré-ponte") since projects can be poultry, pigs, fish or cattle.
 */
public enum ProjectPhase {
    STARTUP,
    RAMP_UP,
    PRE_PRODUCTION,
    STABLE,
    WIND_DOWN
}
