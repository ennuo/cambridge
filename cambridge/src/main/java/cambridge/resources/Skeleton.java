package cambridge.resources;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;

public class Skeleton implements Serializable
{
    public Skeleton(byte[] data)
    {
        this.load(new MemoryInputStream(data));
    }

    public static class Joint
    {
        public transient Skeleton skeleton;


        public int id;
        public int index;
        public int parent, nextSibling, firstChild;
        public Quaternionf rotation;
        public Vector3f translation;
        public Vector3f scale;

        public Matrix4f getLocalTransform()
        {
            return new Matrix4f()
                .identity()
                .translationRotateScale(
                    translation,
                    rotation,
                    scale
                );
        }

        public Matrix4f getGlobalTransform()
        {
            if (this.parent == -1) return this.getLocalTransform();
            ArrayList<Matrix4f> transforms = new ArrayList<>();
            transforms.add(this.getLocalTransform());
            Joint joint = this.skeleton.joints[this.parent];
            while (true)
            {
                transforms.add(joint.getLocalTransform());
                if (joint.parent == -1) break;
                joint = this.skeleton.joints[joint.parent];
            }
            Matrix4f wpos = new Matrix4f();
            for (int i = transforms.size() - 1; i >= 0; --i)
                wpos.mul(transforms.get(i));
            return wpos;
        }

    }

    public int id;
    public Joint[] joints;


    @Override
    public void load(MemoryInputStream stream)
    {
        stream.i32();
        this.id = stream.i32();
        stream.seek(0x2);
        int boneCount = stream.u16();
        int hierachyOffset = stream.getOffset() + stream.i32();
        int transformOffset = stream.getOffset() + stream.i32();
        int idOffset = stream.getOffset() + stream.i32();
        stream.seek(hierachyOffset - stream.getOffset());

        this.joints = new Joint[boneCount];

        for (int i = 0; i < boneCount; ++i)
        {
            Joint joint = new Joint();
            joint.skeleton = this;

            stream.i16();
            joint.parent = stream.i16();
            joint.nextSibling = stream.i16();
            joint.firstChild = stream.i16();

            joints[i] = joint;
        }
        stream.seek(transformOffset - stream.getOffset());
        for (int i = 0; i < boneCount; ++i)
        {
            Joint joint = joints[i];

            joint.rotation = new Quaternionf(stream.f32(), stream.f32(), stream.f32(),
                stream.f32());
            joint.translation = stream.v3();
            stream.f32();
            joint.scale = stream.v3();
            stream.f32();
        }

        stream.seek(idOffset - stream.getOffset());
        for (int i = 0; i < boneCount; ++i)
        {
            int boneId = stream.i32();
            int boneIndex = stream.i32();

            this.joints[boneIndex].id = boneId;
            this.joints[boneIndex].index = boneIndex;
        }
    }
}
