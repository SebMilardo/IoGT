/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unict.dieei.iogt;

import java.util.Arrays;

/**
 *
 * @author Seby
 */
public class SensedData {

    double[] position;
    float[] rotation;

    public SensedData(double[] position, float[] rotation) {
        this.position = position;
        this.rotation = rotation;
    }

    public double[] getPosition() {
        return position;
    }

    public void setPosition(double[] position) {
        this.position = position;
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }

    @Override
    public String toString() {
        return "SensedData{" + "position=" + Arrays.toString(position)
                + ", rotation=" + Arrays.toString(rotation) + '}';
    }

}
