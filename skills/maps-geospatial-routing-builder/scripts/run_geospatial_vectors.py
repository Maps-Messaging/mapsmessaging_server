#!/usr/bin/env python3
from __future__ import annotations

import math

BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz'


def geohash_encode(lat: float, lon: float, precision: int = 5) -> str:
    lat_interval = [-90.0, 90.0]
    lon_interval = [-180.0, 180.0]
    bits = [16, 8, 4, 2, 1]
    bit = 0
    ch = 0
    even = True
    out = []

    while len(out) < precision:
        if even:
            mid = (lon_interval[0] + lon_interval[1]) / 2
            if lon >= mid:
                ch |= bits[bit]
                lon_interval[0] = mid
            else:
                lon_interval[1] = mid
        else:
            mid = (lat_interval[0] + lat_interval[1]) / 2
            if lat >= mid:
                ch |= bits[bit]
                lat_interval[0] = mid
            else:
                lat_interval[1] = mid
        even = not even

        if bit < 4:
            bit += 1
        else:
            out.append(BASE32[ch])
            bit = 0
            ch = 0

    return ''.join(out)


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371.0088
    p1 = math.radians(lat1)
    p2 = math.radians(lat2)
    dlat = p2 - p1
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlon / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return r * c


def check_vectors() -> int:
    # Known geohash prefixes (stable at low precision)
    london = geohash_encode(51.5074, -0.1278, precision=5)
    nyc = geohash_encode(40.7128, -74.0060, precision=5)
    if not london.startswith('gcp'):
        print(f'geohash check failed for London: {london}')
        return 1
    if not nyc.startswith('dr5'):
        print(f'geohash check failed for NYC: {nyc}')
        return 1

    # Known distance sanity: London <-> Paris about 344 km (+/- 20 km bound)
    d_lp = haversine_km(51.5074, -0.1278, 48.8566, 2.3522)
    if not (324 <= d_lp <= 364):
        print(f'distance check failed for London-Paris: {d_lp:.2f} km')
        return 1

    # Order sanity: London->Paris < London->Berlin
    d_lb = haversine_km(51.5074, -0.1278, 52.52, 13.4050)
    if not (d_lp < d_lb):
        print(f'distance order check failed: LP={d_lp:.2f}, LB={d_lb:.2f}')
        return 1

    # Invalid coordinate checks
    invalid_cases = [
        (95.0, 0.0),
        (-95.0, 0.0),
        (0.0, 190.0),
        (0.0, -190.0),
    ]
    for lat, lon in invalid_cases:
        if -90 <= lat <= 90 and -180 <= lon <= 180:
            print(f'invalid vector check failed for ({lat}, {lon})')
            return 1

    print('geospatial vector smoke PASS')
    print(f'london geohash(5)={london}, nyc geohash(5)={nyc}, london-paris={d_lp:.2f} km')
    return 0


if __name__ == '__main__':
    raise SystemExit(check_vectors())
