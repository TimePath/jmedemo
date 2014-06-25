package com.timepath.tafechal14

import com.jme3.input.CameraInput
import com.jme3.input.InputManager
import com.jme3.input.MouseInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.AnalogListener
import com.jme3.input.controls.MouseAxisTrigger
import com.jme3.math.FastMath
import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class CustomCamera implements AnalogListener, ActionListener {

    static String[] MAPPINGS = [
            CameraInput.FLYCAM_LEFT,
            CameraInput.FLYCAM_RIGHT,
            CameraInput.FLYCAM_UP,
            CameraInput.FLYCAM_DOWN,

            CameraInput.FLYCAM_STRAFELEFT,
            CameraInput.FLYCAM_STRAFERIGHT,
            CameraInput.FLYCAM_FORWARD,
            CameraInput.FLYCAM_BACKWARD,
    ]

    Vector3f initialUpVec

    Camera camera

    float sensitivity = 6

    void setCamera(Camera value) {
        this.camera = value
        initialUpVec = value.up.clone()
    }

    boolean sixDof

    float rotationSpeed = 0.022

    /**
     * Speed is given in world units per second.
     */
    float moveSpeed = 30

    /**
     * If false, the camera will ignore input.
     */
    boolean enabled = true

    void setEnabled(boolean value) {
        inputManager?.setCursorVisible(!value)
        enabled = value
    }

    float fov

    /**
     * Derive fovY value
     * @return
     */
    float getFov() {
        float h = camera.frustumTop
        float near = camera.frustumNear
        return (FastMath.atan(h / near as float) / (FastMath.DEG_TO_RAD * 0.5f as float)) as float
    }

    void setFov(float value) {
        if (value <= 0) return
        float w = camera.frustumRight
        float h = camera.frustumTop
        float aspect = w / h as float
        h = FastMath.tan(value * FastMath.DEG_TO_RAD * 0.5f as float) * camera.frustumNear
        w = h * aspect

        camera.frustumTop = h
        camera.frustumBottom = -h
        camera.frustumLeft = -w
        camera.frustumRight = w
    }

    InputManager inputManager

    void setInputManager(InputManager value) {
        if (value == null) return
        inputManager = value
        inputManager.with {
            // Mouse
            addMapping(CameraInput.FLYCAM_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true))
            addMapping(CameraInput.FLYCAM_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false))
            addMapping(CameraInput.FLYCAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false))
            addMapping(CameraInput.FLYCAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true))

            // Keyboard
//            addMapping(CameraInput.FLYCAM_STRAFELEFT, new KeyTrigger(KeyInput.KEY_A))
//            addMapping(CameraInput.FLYCAM_STRAFERIGHT, new KeyTrigger(KeyInput.KEY_D))
//            addMapping(CameraInput.FLYCAM_FORWARD, new KeyTrigger(KeyInput.KEY_W))
//            addMapping(CameraInput.FLYCAM_BACKWARD, new KeyTrigger(KeyInput.KEY_S))

            addListener(this, MAPPINGS)
            setCursorVisible(false)
        }
    }

    protected void rotate(float value, Vector3f axis) {
        def up = camera.up, left = camera.left, dir = camera.direction
        def matrix = new Matrix3f(), quaternion = new Quaternion()
        matrix.with {
            fromAngleNormalAxis(value * 3 * FastMath.TWO_PI * rotationSpeed * sensitivity as float, axis)
            mult(up, up)
            mult(left, left)
            mult(dir, dir)
        }
        quaternion.with {
            fromAxes(left, up, dir)
            normalizeLocal()
        }
        camera.setAxes(quaternion)
    }

    protected void move(float value, Vector3f axis) {
        camera.location = camera.location.add(axis.mult(value * moveSpeed as float))
    }

    @Override
    void onAnalog(String name, float value, float tpf) {
        if (!enabled) return
        switch (name) {
            case CameraInput.FLYCAM_LEFT: rotate(value, sixDof ? camera.up : initialUpVec); break
            case CameraInput.FLYCAM_RIGHT: rotate(-value, sixDof ? camera.up : initialUpVec); break
            case CameraInput.FLYCAM_UP: rotate(-value, camera.left); break
            case CameraInput.FLYCAM_DOWN: rotate(value, camera.left); break
            case CameraInput.FLYCAM_FORWARD: move(value, camera.direction); break
            case CameraInput.FLYCAM_BACKWARD: move(-value, camera.direction); break
            case CameraInput.FLYCAM_STRAFELEFT: move(value, camera.left); break
            case CameraInput.FLYCAM_STRAFERIGHT: move(-value, camera.left); break
        }
    }

    @Override
    void onAction(String name, boolean value, float tpf) {

    }

}
