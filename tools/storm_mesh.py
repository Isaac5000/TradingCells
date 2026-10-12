"""Closed, low-poly weapon meshes baked by NeoForge's standard OBJ loader."""
import math


def sub(a, b):
    return [x - y for x, y in zip(a, b)]


def cross(a, b):
    return [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]


def export(path, parts):
    lines = ["mtllib industrial.mtl", "usemtl metal"]
    index = 0

    def face(points, uv, outward):
        nonlocal index
        normal = cross(sub(points[1], points[0]), sub(points[2], points[0]))
        if sum(x*y for x, y in zip(normal, outward)) < 0:
            points, uv = points[::-1], uv[::-1]
        for p, t in zip(points, uv):
            lines.append("v " + " ".join(f"{v / 16:.7f}" for v in p))
            lines.append(f"vt {t[0] / 16:.7f} {t[1] / 16:.7f}")
        ids = list(range(index + 1, index + len(points) + 1))
        if len(ids) == 3:
            ids.append(ids[-1])
        lines.append("f " + " ".join(f"{i}/{i}" for i in ids))
        index += len(points)

    for part in parts:
        if "prism" in part:
            axis, center, low, high, radius, tile = part["prism"]
            radial = [i for i in range(3) if i != axis]
            ring = []
            for n in range(8):
                angle = math.radians(22.5 + 45*n)
                point = list(center)
                point[radial[0]] += radius * math.cos(angle) / math.cos(math.pi/8)
                point[radial[1]] += radius * math.sin(angle) / math.cos(math.pi/8)
                ring.append(point)
            def at(p, depth):
                result = list(p)
                result[axis] = depth
                return result
            u, v = tile % 4 * 4, tile // 4 * 8
            for n in range(8):
                a, b = ring[n], ring[(n+1) % 8]
                outward = [(a[i]+b[i])/2-center[i] if i != axis else 0 for i in range(3)]
                width = math.dist(a, b)
                scale = min(1, 8 / max(high-low, width))
                face([at(a, low), at(b, low), at(b, high), at(a, high)],
                     [(u,v), (u+width*scale/2,v), (u+width*scale/2,v+(high-low)*scale), (u,v+(high-low)*scale)], outward)
            # Triangular fans close the entire octagon, including its bevelled corners.
            for depth, sign in ((low, -1), (high, 1)):
                outward = [sign if i == axis else 0 for i in range(3)]
                for n in range(8):
                    points = [at(center, depth), at(ring[n], depth), at(ring[(n+1) % 8], depth)]
                    scale = min(1, 3.5 / radius)
                    uv = [(u+2+(p[radial[0]]-center[radial[0]])*scale/2,
                           v+4+(p[radial[1]]-center[radial[1]])*scale) for p in points]
                    face(points, uv, outward)
            continue
        a, b = part["from"], part["to"]
        for side, axis, positive in (("west",0,False),("east",0,True),("down",1,False),
                                     ("up",1,True),("north",2,False),("south",2,True)):
            if side not in part["faces"]:
                continue
            other = [i for i in range(3) if i != axis]
            points = []
            for first, second in ((0,0),(1,0),(1,1),(0,1)):
                p = list(a)
                p[axis] = b[axis] if positive else a[axis]
                p[other[0]] = b[other[0]] if first else a[other[0]]
                p[other[1]] = b[other[1]] if second else a[other[1]]
                points.append(p)
            outward = [int(positive)*2-1 if i == axis else 0 for i in range(3)]
            if "rotation" in part:
                rotation = part["rotation"]
                raxis = "xyz".index(rotation["axis"])
                angle = math.radians(rotation["angle"])
                def rotate(p, origin):
                    p = sub(p, origin)
                    x, y = (raxis+1)%3, (raxis+2)%3
                    p[x], p[y] = p[x]*math.cos(angle)-p[y]*math.sin(angle), p[x]*math.sin(angle)+p[y]*math.cos(angle)
                    return [x+y for x,y in zip(p, origin)]
                points = [rotate(p, rotation["origin"]) for p in points]
                outward = rotate(outward, [0,0,0])
            u,v,x,y = part["faces"][side]["uv"]
            face(points, [(u,v),(x,v),(x,y),(u,y)], outward)
    path.write_text("\n".join(lines)+"\n", encoding="ascii")
    path.with_name("industrial.mtl").write_text("newmtl metal\nKd 1 1 1\nKa 0 0 0\nmap_Kd #atlas\n", encoding="ascii")
