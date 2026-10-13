/**
 * Background photo for each page, loaded from the Pexels CDN (free to use, no attribution required).
 * Photo pages: https://www.pexels.com/photo/<id>/
 */
const pexels = (id: number): string => `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&cs=tinysrgb&w=1600`;

export const PAGE_IMAGES: Record<string, string> = {
  '/': pexels(33706880), // control room interior
  '/analytics': pexels(11169626), // electricity pylon at dusk
  '/faults': pexels(5707737), // lightning over power lines
  '/network': pexels(36566099), // high-voltage transmission towers at sunset
  '/optimization': pexels(18535002), // transmission towers and power lines over a forest
  '/np': pexels(31748138), // lineman working on a power line
  '/reports': pexels(18468536), // power distribution substation
  '/parallel': pexels(11924298), // row of electricity meters
  '/benchmarks': pexels(20442922), // circuit board close-up
};
