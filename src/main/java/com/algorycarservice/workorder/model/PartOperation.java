package com.algorycarservice.workorder.model;

/** What was done to a {@link VehiclePart}; the body-shop terms follow the usual "değişen / boyalı" report. */
public enum PartOperation {
    REPLACED,
    PAINTED,
    LOCAL_PAINT,
    REPAIRED,
    SERVICED
}
