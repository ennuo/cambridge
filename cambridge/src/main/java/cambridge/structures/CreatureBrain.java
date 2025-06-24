package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class CreatureBrain extends GameObject
{
    public int behavior;
    public float radius;
    public int vulnerable;
    public float movement_speed;
    public float jump_interval;
    public float jump_phase;
    public float jump_modifier;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.radius = stream.f32();
        this.behavior = stream.i32();
        this.vulnerable = stream.i32();
        this.movement_speed = stream.f32();
        this.jump_interval = stream.f32();
        this.jump_phase = stream.f32();
        this.jump_modifier = stream.f32();
    }
}
