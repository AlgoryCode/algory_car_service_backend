package com.algorycarservice.vehiclemark.model;

/**
 * Where a mark sits: {@link #MODEL} is the 3D model (x, y and z across its bounding box), the others are the flat
 * drawings of earlier app versions (x and y only).
 */
public enum CarView {
    MODEL,
    TOP,
    LEFT,
    RIGHT,
    FRONT,
    REAR
}
