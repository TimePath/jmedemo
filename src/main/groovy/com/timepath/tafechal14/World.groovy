package com.timepath.tafechal14

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
import com.jme3.util.BufferUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class World {

    static boolean[][][] generate(int width, int height, int depth, int straightness, Random r = new Random()) {
        boolean[][][] map = new boolean[depth][height][width]
        int n = (width * height * depth) / 3 as int
        int x = 0, y = 0, z = 0, straight = 0, direction = 0
        for (int i = 0; i < n; i++) {
            map[z][y][x] = true
            if (--straight < 0) {
                straight = straightness + (r.nextInt(2) * (straightness / 2)) as int
                direction = (direction + (1 + r.nextInt(5))) % 6; // new direction, not behind
            }
            switch (direction) {
                case 0: if (++x >= width) x %= width; break
                case 1: if (++y >= height) y %= height; break
                case 2: if (++z >= depth) z %= depth; break
                case 3: if (--x < 0) x += width; break
                case 4: if (--y < 0) y += height; break
                case 5: if (--z < 0) z += depth; break
            }
        }
        return map
    }

    static Node node(Material mat,
                     int width = 10, int height = 10, int depth = 10,
                     int straightness = 2, float units = 10, int divisions = 1) {
        boolean[][][] hollow = generate(width, height, depth, straightness)
        def level = new Node("Chunks")
        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                for (int k = 0; k < divisions; k++) {
                    def shape = new CompoundCollisionShape()
                    def mesh = mesh(hollow,
                            i * (width / divisions) as int,
                            width / divisions as int,
                            j * (height / divisions) as int,
                            height / divisions as int,
                            k * (depth / divisions) as int,
                            depth / divisions as int,
                            units as int,
                            shape)
                    if (mesh == null) continue
                    mesh.scaleTextureCoordinates(new Vector2f(units / 4 as float, units / 4 as float))
                    def geom = new Geometry("Chunk ${k}.${j}.${i}", mesh)
                    def rbc = new RigidBodyControl(shape, 0)
                    rbc.with {
                        setKinematic(true)
                    }
                    geom.with {
                        setMaterial(mat)
                        setLocalTranslation(
                                i * (width / divisions) * units as float,
                                j * (height / divisions) * units as float,
                                k * (depth / divisions) * units as float
                        )
                        addControl(rbc)
                    }
                    level.attachChild(geom)
                }
            }
        }
        return level
    }

    static Mesh mesh(boolean[][][] hollow,
                     int widthOffset, int width,
                     int heightOffset, int height,
                     int depthOffset, int depth,
                     int units, boolean rle = false,
                     CompoundCollisionShape shape) {
        def axes = [Vector3f.UNIT_X.mult(units), Vector3f.UNIT_Y.mult(units), Vector3f.UNIT_Z.mult(units)]
        def mesh = new Mesh()
        def verts = BufferUtils.createVector3Buffer(6 * 4 * depth * height * width)
        def indices = BufferUtils.createIntBuffer(6 * 6 * depth * height * width)
        def normals = BufferUtils.createFloatBuffer(6 * 12 * depth * height * width)
        def uv = BufferUtils.createFloatBuffer(6 * 8 * depth * height * width)
        Vector3f pos = new Vector3f(), start = null, end = new Vector3f()
        int idx = 0
        for (int i in depthOffset..<depthOffset + depth) {
            for (int j in heightOffset..<heightOffset + height) {
                for (int k in widthOffset..<widthOffset + width) {
                    pos.set(
                            (k - widthOffset) * units as float,
                            (j - heightOffset) * units as float,
                            (i - depthOffset) * units as float)
                    def current = hollow[i][j][k]
                    def back = hollow[(i + depth - 1) % depth][j][k]
                    def front = hollow[(i + 1) % depth][j][k]
                    def top = hollow[i][(j + 1) % height][k]
                    def bottom = hollow[i][(j + height - 1) % height][k]
                    def left = hollow[i][j][(k + width - 1) % width]
                    def right = hollow[i][j][(k + 1) % width]
                    if (start != null && (!rle || current || start.y != pos.y)) {
                        Vector3f size = end.subtract(start)
                        Vector3f half = new Vector3f(
                                0.5f * (size.x + units) as float,
                                0.5f * (size.y + units) as float,
                                0.5f * (size.z + units) as float
                        )
                        shape.addChildShape(new BoxCollisionShape(half), start.add(half))
                        start = null
                    }
                    if (!current) {
                        if (!front && !back && !left && !right && !top && !bottom) continue; // ignore unreachable
                        if (start == null) start = new Vector3f(pos)
                        end.set(pos)
                        continue
                    }
                    Vector3f[] v = [
                            // back: >_]
                            pos, // 0
                            pos.add(axes[0]), // +x
                            pos.add(axes[0]).addLocal(axes[1]), // +x+y
                            pos.add(axes[1]), // +y
                            // front: Z_<
                            // 4
                            pos.add(axes[0]).addLocal(axes[2]), // +x+z
                            pos.add(axes[2]), // +z
                            pos.add(axes[0]).addLocal(axes[1]).addLocal(axes[2]), // +x+y+z
                            pos.add(axes[1]).addLocal(axes[2]) // +y+z
                    ]
                    if (!back) {
                        verts.put([
                                v[0].x, v[0].y, v[0].z,
                                v[1].x, v[1].y, v[1].z,
                                v[2].x, v[2].y, v[2].z,
                                v[3].x, v[3].y, v[3].z
                        ] as float[])
                        indices.put([
                                idx, idx + 1, idx + 2,
                                idx, idx + 2, idx + 3
                        ] as int[])
                        idx += 4
                        normals.put([
                                0, 0, 1,
                                0, 0, 1,
                                0, 0, 1,
                                0, 0, 1
                        ] as float[])
                        uv.put([
                                0, 0,
                                1, 0,
                                1, 1,
                                0, 1
                        ] as float[])
                    }
                    if (!front) {
                        verts.put([
                                v[4].x, v[4].y, v[4].z,
                                v[5].x, v[5].y, v[5].z,
                                v[7].x, v[7].y, v[7].z,
                                v[6].x, v[6].y, v[6].z
                        ] as float[])
                        indices.put([
                                idx, idx + 1, idx + 2,
                                idx, idx + 2, idx + 3
                        ] as int[])
                        idx += 4
                        normals.put([
                                0, 0, -1,
                                0, 0, -1,
                                0, 0, -1,
                                0, 0, -1
                        ] as float[])
                        uv.put([
                                0, 0,
                                1, 0,
                                1, 1,
                                0, 1
                        ] as float[])
                    }
                    if (!top) {
                        verts.put([
                                v[2].x, v[2].y, v[2].z,
                                v[6].x, v[6].y, v[6].z,
                                v[7].x, v[7].y, v[7].z,
                                v[3].x, v[3].y, v[3].z
                        ] as float[])
                        indices.put([
                                idx, idx + 1, idx + 2,
                                idx, idx + 2, idx + 3
                        ] as int[])
                        idx += 4
                        normals.put([
                                0, -1, 0,
                                0, -1, 0,
                                0, -1, 0,
                                0, -1, 0
                        ] as float[])
                        uv.put([
                                0, 1,
                                0, 0,
                                1, 0,
                                1, 1
                        ] as float[])
                    }
                    if (!bottom) {
                        verts.put([
                                v[0].x, v[0].y, v[0].z,
                                v[5].x, v[5].y, v[5].z,
                                v[4].x, v[4].y, v[4].z,
                                v[1].x, v[1].y, v[1].z
                        ] as float[])
                        indices.put([
                                idx, idx + 1, idx + 2,
                                idx, idx + 2, idx + 3
                        ] as int[])
                        idx += 4
                        normals.put([
                                0, 1, 0,
                                0, 1, 0,
                                0, 1, 0,
                                0, 1, 0
                        ] as float[])
                        uv.put([
                                1, 0,
                                1, 1,
                                0, 1,
                                0, 0
                        ] as float[])
                    }
                    if (!left) {
                        verts.put([
                                v[5].x, v[5].y, v[5].z,
                                v[0].x, v[0].y, v[0].z,
                                v[3].x, v[3].y, v[3].z,
                                v[7].x, v[7].y, v[7].z
                        ] as float[])
                        indices.put([
                                idx, idx + 1, idx + 2,
                                idx, idx + 2, idx + 3
                        ] as int[])
                        idx += 4
                        normals.put([
                                1, 0, 0,
                                1, 0, 0,
                                1, 0, 0,
                                1, 0, 0
                        ] as float[])
                        uv.put([
                                0, 0,
                                1, 0,
                                1, 1,
                                0, 1
                        ] as float[])
                    }
                    if (!right) {
                        verts.put([
                                v[1].x, v[1].y, v[1].z,
                                v[4].x, v[4].y, v[4].z,
                                v[6].x, v[6].y, v[6].z,
                                v[2].x, v[2].y, v[2].z
                        ] as float[])
                        indices.put([
                                idx, idx + 1, idx + 2,
                                idx, idx + 2, idx + 3
                        ] as int[])
                        idx += 4
                        normals.put([
                                -1, 0, 0,
                                -1, 0, 0,
                                -1, 0, 0,
                                -1, 0, 0
                        ] as float[])
                        uv.put([
                                0, 0,
                                1, 0,
                                1, 1,
                                0, 1
                        ] as float[])
                    }
                }
            }
        }
        mesh.setBuffer(Type.Position, 3, verts)
        mesh.setBuffer(Type.Index, 3, indices)
        mesh.setBuffer(Type.Normal, 3, normals)
        mesh.setBuffer(Type.TexCoord, 2, uv)
        mesh.bound = new BoundingBox(Vector3f.ZERO, new Vector3f(width * units, height * units, depth * units))
        mesh.setStatic()
        return mesh
    }
}
