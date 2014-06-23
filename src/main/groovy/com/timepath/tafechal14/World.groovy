package com.timepath.tafechal14

import com.jme3.asset.AssetManager
import com.jme3.bounding.BoundingBox
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.control.RigidBodyControl
import com.jme3.material.Material
import com.jme3.math.Vector2f
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer.Type
import com.jme3.texture.Texture
import com.jme3.texture.Texture.WrapMode
import com.jme3.util.BufferUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import java.nio.FloatBuffer
import java.nio.IntBuffer

@CompileStatic
@TypeChecked
@Log('LOG')
public class World {

    static boolean[][][] hollow(int width, int height, int depth, int straightness) {
        return hollow(width, height, depth, straightness, new Random());
    }

    static boolean[][][] hollow(int width, int height, int depth, int straightness, Random r) {
        boolean[][][] hollow = new boolean[depth][height][width];
        int n = (width * height * depth) / 3 as int;
        int x = 0, y = 0, z = 0, straight = 0, direction = 0;
        for (int i = 0; i < n; i++) {
            hollow[z][y][x] = true;
            if (--straight < 0) {
                straight = straightness + (r.nextInt(2) * (straightness / 2)) as int;
                direction = (direction + (1 + r.nextInt(5))) % 6; // new direction, not behind
            }
            switch (direction) {
                case 0:
                    if (++x >= width) x %= width;
                    break;
                case 1:
                    if (++y >= height) y %= height;
                    break;
                case 2:
                    if (++z >= depth) z %= depth;
                    break;
                case 3:
                    if (--x < 0) x += width;
                    break;
                case 4:
                    if (--y < 0) y += height;
                    break;
                case 5:
                    if (--z < 0) z += depth;
                    break;
            }
        }
        return hollow;
    }

    public static Node generate(AssetManager assetManager) {
        return generate(assetManager, 10, 10, 10, 10);
    }

    public static Node generate(AssetManager assetManager, int width, int height, int depth, float units) {
        boolean[][][] hollow = hollow(width, height, depth, 2);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture tex = assetManager.loadTexture("Textures/wall.png");
        tex.setWrap(WrapMode.Repeat);
        mat.setBoolean("VertexLighting", false);
        mat.setBoolean("HighQuality", true);
        mat.setBoolean("LowQuality", false);
        mat.setTexture("DiffuseMap", tex);
        Node level = new Node("Chunks");
        int n = 1;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    CompoundCollisionShape shape = new CompoundCollisionShape();
                    Mesh mesh = mesh(hollow,
                            i * (width / n) as int,
                            width / n as int,
                            j * (height / n) as int,
                            height / n as int,
                            k * (depth / n) as int,
                            depth / n as int,
                            units as int,
                            shape);
                    if (mesh == null) continue;
                    mesh.scaleTextureCoordinates(new Vector2f(units / 4 as float, units / 4 as float));
                    Geometry geom = new Geometry("Chunk " + k + "." + j + "." + i, mesh);
                    geom.setMaterial(mat);
                    geom.setLocalTranslation(i * (width / n) * units as float,
                            j * (height / n) * units as float,
                            k * (depth / n) * units as float);
                    geom.addControl(new RigidBodyControl(shape, 0));
                    level.attachChild(geom);
                }
            }
        }
        return level;
    }

    private static Mesh mesh(boolean[][][] hollow,
                             int widthOffset,
                             int width,
                             int heightOffset,
                             int height,
                             int depthOffset,
                             int depth,
                             int units,
                             CompoundCollisionShape shape) {
        Mesh mesh = new Mesh();
        FloatBuffer verts = BufferUtils.createVector3Buffer(6 * 4 * depth * height * width);
        IntBuffer indices = BufferUtils.createIntBuffer(6 * 6 * depth * height * width);
        FloatBuffer normals = BufferUtils.createFloatBuffer(6 * 12 * depth * height * width);
        FloatBuffer uv = BufferUtils.createFloatBuffer(6 * 8 * depth * height * width);
        Vector3f pos = new Vector3f();
        Vector3f[] axes = [
                Vector3f.UNIT_X.mult(units), Vector3f.UNIT_Y.mult(units), Vector3f.UNIT_Z.mult(units)
        ];
        Vector3f start = null, end = new Vector3f();
        boolean rle = false;
        int idx = 0;
        for (int i = depthOffset; i < depthOffset + depth; i++) {
            for (int j = heightOffset; j < heightOffset + height; j++) {
                for (int k = widthOffset; k < widthOffset + width; k++) {
                    pos.set((k - widthOffset) * units, (j - heightOffset) * units, (i - depthOffset) * units);
                    boolean current = hollow[i][j][k];
                    boolean back = hollow[(i + depth - 1) % depth][j][k];
                    boolean front = hollow[(i + 1) % depth][j][k];
                    boolean top = hollow[i][(j + 1) % height][k];
                    boolean bottom = hollow[i][(j + height - 1) % height][k];
                    boolean left = hollow[i][j][(k + width - 1) % width];
                    boolean right = hollow[i][j][(k + 1) % width];
                    if (start != null && (!rle || current || start.y != pos.y)) {
                        Vector3f size = end.subtract(start);
                        Vector3f half = new Vector3f(0.5f * (size.x + units) as float,
                                0.5f * (size.y + units) as float,
                                0.5f * (size.z + units) as float);
                        shape.addChildShape(new BoxCollisionShape(half), start.add(half));
                        start = null;
                    }
                    if (!current) {
                        if (!front && !back && !left && !right && !top && !bottom) continue; // ignore unreachable
                        if (start == null) start = new Vector3f(pos);
                        end.set(pos);
                        continue;
                    }
                    Vector3f[] v = [
                            // back: >_]
                            pos, //
                            pos.add(axes[0]), // +x
                            pos.add(axes[0]).addLocal(axes[1]), // +x+y
                            pos.add(axes[1]), // +y
                            // front: Z_<
                            // 4
                            pos.add(axes[0]).addLocal(axes[2]), // +x+z
                            pos.add(axes[2]), // +z
                            pos.add(axes[0]).addLocal(axes[1]).addLocal(axes[2]), // +x+y+z
                            pos.add(axes[1]).addLocal(axes[2]) // +y+z
                    ];
                    if (!back) {
                        verts.put([
                                v[0].x,
                                v[0].y,
                                v[0].z,
                                v[1].x,
                                v[1].y,
                                v[1].z,
                                v[2].x,
                                v[2].y,
                                v[2].z,
                                v[3].x,
                                v[3].y,
                                v[3].z
                        ] as float[]);
                        indices.put([
                                idx, idx + 1, idx + 2, idx, idx + 2, idx + 3
                        ] as int[]);
                        idx += 4;
                        normals.put([
                                0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1
                        ] as float[]);
                        uv.put([
                                0, 0, 1, 0, 1, 1, 0, 1
                        ] as float[]);
                    }
                    if (!front) {
                        verts.put([
                                v[4].x,
                                v[4].y,
                                v[4].z,
                                v[5].x,
                                v[5].y,
                                v[5].z,
                                v[7].x,
                                v[7].y,
                                v[7].z,
                                v[6].x,
                                v[6].y,
                                v[6].z
                        ] as float[]);
                        indices.put([
                                idx, idx + 1, idx + 2, idx, idx + 2, idx + 3
                        ] as int[]);
                        idx += 4;
                        normals.put([
                                0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1
                        ] as float[]);
                        uv.put([
                                0, 0, 1, 0, 1, 1, 0, 1
                        ] as float[]);
                    }
                    if (!top) {
                        verts.put([
                                v[2].x,
                                v[2].y,
                                v[2].z,
                                v[6].x,
                                v[6].y,
                                v[6].z,
                                v[7].x,
                                v[7].y,
                                v[7].z,
                                v[3].x,
                                v[3].y,
                                v[3].z
                        ] as float[]);
                        indices.put([
                                idx, idx + 1, idx + 2, idx, idx + 2, idx + 3
                        ] as int[]);
                        idx += 4;
                        normals.put([
                                0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0
                        ] as float[]);
                        uv.put([
                                0, 1, 0, 0, 1, 0, 1, 1
                        ] as float[]);
                    }
                    if (!bottom) {
                        verts.put([
                                v[0].x,
                                v[0].y,
                                v[0].z,
                                v[5].x,
                                v[5].y,
                                v[5].z,
                                v[4].x,
                                v[4].y,
                                v[4].z,
                                v[1].x,
                                v[1].y,
                                v[1].z
                        ] as float[]);
                        indices.put([
                                idx, idx + 1, idx + 2, idx, idx + 2, idx + 3
                        ] as int[]);
                        idx += 4;
                        normals.put([
                                0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0
                        ] as float[]);
                        uv.put([
                                1, 0, 1, 1, 0, 1, 0, 0
                        ] as float[]);
                    }
                    if (!left) {
                        verts.put([
                                v[5].x,
                                v[5].y,
                                v[5].z,
                                v[0].x,
                                v[0].y,
                                v[0].z,
                                v[3].x,
                                v[3].y,
                                v[3].z,
                                v[7].x,
                                v[7].y,
                                v[7].z
                        ] as float[]);
                        indices.put([
                                idx, idx + 1, idx + 2, idx, idx + 2, idx + 3
                        ] as int[]);
                        idx += 4;
                        normals.put([
                                1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0
                        ] as float[]);
                        uv.put([
                                0, 0, 1, 0, 1, 1, 0, 1
                        ] as float[]);
                    }
                    if (!right) {
                        verts.put([
                                v[1].x,
                                v[1].y,
                                v[1].z,
                                v[4].x,
                                v[4].y,
                                v[4].z,
                                v[6].x,
                                v[6].y,
                                v[6].z,
                                v[2].x,
                                v[2].y,
                                v[2].z
                        ] as float[]);
                        indices.put([
                                idx, idx + 1, idx + 2, idx, idx + 2, idx + 3
                        ] as int[]);
                        idx += 4;
                        normals.put([
                                -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0
                        ] as float[]);
                        uv.put([
                                0, 0, 1, 0, 1, 1, 0, 1
                        ] as float[]);
                    }
                }
            }
        }
        mesh.setBuffer(Type.Position, 3, verts);
        mesh.setBuffer(Type.Index, 3, indices);
        mesh.setBuffer(Type.Normal, 3, normals);
        mesh.setBuffer(Type.TexCoord, 2, uv);
        mesh.setBound(new BoundingBox(Vector3f.ZERO, new Vector3f(width * units, height * units, depth * units)));
        mesh.setStatic();
        return mesh;
    }
}
