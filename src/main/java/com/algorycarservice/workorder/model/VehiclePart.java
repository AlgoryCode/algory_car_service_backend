package com.algorycarservice.workorder.model;

/**
 * Where on the vehicle a work order line was done. Body panels, glass and wheels are drawn on the vehicle diagram;
 * the mechanical groups are listed under it.
 */
public enum VehiclePart {
    FRONT_BUMPER,
    HOOD,
    WINDSHIELD,
    ROOF,
    REAR_WINDOW,
    TRUNK,
    REAR_BUMPER,
    FRONT_LEFT_FENDER,
    FRONT_LEFT_DOOR,
    REAR_LEFT_DOOR,
    REAR_LEFT_FENDER,
    FRONT_RIGHT_FENDER,
    FRONT_RIGHT_DOOR,
    REAR_RIGHT_DOOR,
    REAR_RIGHT_FENDER,
    FRONT_LEFT_WHEEL,
    FRONT_RIGHT_WHEEL,
    REAR_LEFT_WHEEL,
    REAR_RIGHT_WHEEL,
    ENGINE,
    TRANSMISSION,
    BRAKES,
    SUSPENSION,
    COOLING,
    EXHAUST,
    ELECTRICAL,
    AIR_CONDITIONING,
    INTERIOR
}
