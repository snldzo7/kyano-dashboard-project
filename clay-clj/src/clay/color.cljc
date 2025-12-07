(ns clay.color
  "Comprehensive color system with Tailwind, HTML, Pantone colors and color modes"
  (:require [hyperfiddle.rcf :refer [tests]]))

#?(:clj (hyperfiddle.rcf/enable!))

;; ============================================================================
;; COLOR SPACE CONVERTERS
;; ============================================================================

(defn clamp [v min-v max-v]
  (max min-v (min max-v v)))

;; Cross-platform math helpers
(defn- to-radians [degrees]
  (* degrees (/ Math/PI 180)))

(defn- to-degrees [radians]
  (* radians (/ 180 Math/PI)))

(defn- parse-int-hex [s]
  #?(:clj (Integer/parseInt s 16)
     :cljs (js/parseInt s 16)))

(defn rgb
  "Create RGB color. Values 0-255."
  ([r g b] (rgb r g b 255))
  ([r g b a]
   {:r (clamp r 0 255)
    :g (clamp g 0 255)
    :b (clamp b 0 255)
    :a (clamp a 0 255)}))

(defn rgba
  "Alias for rgb with explicit alpha"
  [r g b a]
  (rgb r g b a))

(defn hsl->rgb
  "Convert HSL to RGB. H: 0-360, S/L: 0-1"
  ([h s l] (hsl->rgb h s l 1))
  ([h s l a]
   (let [c (* (- 1 (Math/abs (double (- (* 2 l) 1)))) s)
         x (* c (- 1 (Math/abs (double (- (mod (/ h 60) 2) 1)))))
         m (- l (/ c 2))
         [r' g' b'] (cond
                      (< h 60)  [c x 0]
                      (< h 120) [x c 0]
                      (< h 180) [0 c x]
                      (< h 240) [0 x c]
                      (< h 300) [x 0 c]
                      :else     [c 0 x])]
     (rgb (Math/round (double (* (+ r' m) 255)))
          (Math/round (double (* (+ g' m) 255)))
          (Math/round (double (* (+ b' m) 255)))
          (Math/round (double (* a 255)))))))

(defn hsl
  "Create color from HSL. H: 0-360, S/L: 0-100 or 0-1"
  ([h s l] (hsl h s l 1))
  ([h s l a]
   (let [s' (if (> s 1) (/ s 100) s)
         l' (if (> l 1) (/ l 100) l)]
     (hsl->rgb h s' l' a))))

(defn oklch->rgb
  "Convert OKLCH to RGB. L: 0-1, C: 0-0.4, H: 0-360"
  ([l c h] (oklch->rgb l c h 1))
  ([l c h a]
   ;; Simplified OKLCH to sRGB conversion
   ;; For production, use proper color science library
   (let [;; OKLCH to OKLab
         h-rad (to-radians h)
         a' (* c (Math/cos h-rad))
         b' (* c (Math/sin h-rad))
         ;; OKLab to linear sRGB (simplified)
         l' (+ l (* 0.3963377774 a') (* 0.2158037573 b'))
         m' (+ l (* -0.1055613458 a') (* -0.0638541728 b'))
         s' (+ l (* -0.0894841775 a') (* -1.2914855480 b'))
         ;; Cube
         l3 (* l' l' l')
         m3 (* m' m' m')
         s3 (* s' s' s')
         ;; Linear RGB
         r-lin (+ (* 4.0767416621 l3) (* -3.3077115913 m3) (* 0.2309699292 s3))
         g-lin (+ (* -1.2684380046 l3) (* 2.6097574011 m3) (* -0.3413193965 s3))
         b-lin (+ (* -0.0041960863 l3) (* -0.7034186147 m3) (* 1.7076147010 s3))
         ;; Gamma correction
         gamma (fn [x] (if (<= x 0.0031308)
                         (* 12.92 x)
                         (- (* 1.055 (Math/pow x (/ 1 2.4))) 0.055)))]
     (rgb (Math/round (double (* (clamp (gamma r-lin) 0 1) 255)))
          (Math/round (double (* (clamp (gamma g-lin) 0 1) 255)))
          (Math/round (double (* (clamp (gamma b-lin) 0 1) 255)))
          (Math/round (double (* a 255)))))))

(defn oklch
  "Create color from OKLCH"
  ([l c h] (oklch l c h 1))
  ([l c h a] (oklch->rgb l c h a)))

(defn hex->rgb
  "Convert hex string to RGB"
  [hex]
  (let [hex (if (= \# (first hex)) (subs hex 1) hex)
        len (count hex)]
    (cond
      (= 3 len)
      (rgb (parse-int-hex (str (nth hex 0) (nth hex 0)))
           (parse-int-hex (str (nth hex 1) (nth hex 1)))
           (parse-int-hex (str (nth hex 2) (nth hex 2))))

      (= 6 len)
      (rgb (parse-int-hex (subs hex 0 2))
           (parse-int-hex (subs hex 2 4))
           (parse-int-hex (subs hex 4 6)))

      (= 8 len)
      (rgb (parse-int-hex (subs hex 0 2))
           (parse-int-hex (subs hex 2 4))
           (parse-int-hex (subs hex 4 6))
           (parse-int-hex (subs hex 6 8)))

      :else (rgb 0 0 0))))

(defn hex
  "Create color from hex string"
  [s]
  (hex->rgb s))

(defn hsv->rgb
  "Convert HSV/HSB to RGB. H: 0-360, S/V: 0-1"
  ([h s v] (hsv->rgb h s v 1))
  ([h s v a]
   (let [c (* v s)
         x (* c (- 1 (Math/abs (- (mod (/ h 60) 2) 1))))
         m (- v c)
         [r' g' b'] (cond
                      (< h 60)  [c x 0]
                      (< h 120) [x c 0]
                      (< h 180) [0 c x]
                      (< h 240) [0 x c]
                      (< h 300) [x 0 c]
                      :else     [c 0 x])]
     (rgb (Math/round (double (* (+ r' m) 255)))
          (Math/round (double (* (+ g' m) 255)))
          (Math/round (double (* (+ b' m) 255)))
          (Math/round (double (* a 255)))))))

(defn hsv
  "Create color from HSV/HSB. H: 0-360, S/V: 0-100 or 0-1"
  ([h s v] (hsv h s v 1))
  ([h s v a]
   (let [s' (if (> s 1) (/ s 100) s)
         v' (if (> v 1) (/ v 100) v)]
     (hsv->rgb h s' v' a))))

(def hsb hsv) ; Alias

(defn cmyk->rgb
  "Convert CMYK to RGB. C/M/Y/K: 0-1 or 0-100"
  ([c m y k] (cmyk->rgb c m y k 1))
  ([c m y k a]
   (let [c' (if (> c 1) (/ c 100) c)
         m' (if (> m 1) (/ m 100) m)
         y' (if (> y 1) (/ y 100) y)
         k' (if (> k 1) (/ k 100) k)
         r (* 255 (- 1 c') (- 1 k'))
         g (* 255 (- 1 m') (- 1 k'))
         b (* 255 (- 1 y') (- 1 k'))]
     (rgb (Math/round (double r)) (Math/round (double g)) (Math/round (double b)) (Math/round (double (* a 255)))))))

(defn cmyk
  "Create color from CMYK"
  ([c m y k] (cmyk c m y k 1))
  ([c m y k a] (cmyk->rgb c m y k a)))

(defn rgb->cmyk
  "Convert RGB to CMYK. Returns [c m y k] as 0-1"
  [{:keys [r g b]}]
  (let [r' (/ r 255)
        g' (/ g 255)
        b' (/ b 255)
        k (- 1 (max r' g' b'))]
    (if (= k 1)
      [0 0 0 1]
      (let [c (/ (- 1 r' k) (- 1 k))
            m (/ (- 1 g' k) (- 1 k))
            y (/ (- 1 b' k) (- 1 k))]
        [c m y k]))))

(defn rgb->xyz
  "Convert RGB to CIE XYZ"
  [{:keys [r g b]}]
  (let [;; Linearize sRGB
        linearize (fn [v]
                    (let [v' (/ v 255)]
                      (if (<= v' 0.04045)
                        (/ v' 12.92)
                        (Math/pow (/ (+ v' 0.055) 1.055) 2.4))))
        r-lin (linearize r)
        g-lin (linearize g)
        b-lin (linearize b)
        ;; RGB to XYZ (D65 illuminant)
        x (+ (* 0.4124564 r-lin) (* 0.3575761 g-lin) (* 0.1804375 b-lin))
        y (+ (* 0.2126729 r-lin) (* 0.7151522 g-lin) (* 0.0721750 b-lin))
        z (+ (* 0.0193339 r-lin) (* 0.1191920 g-lin) (* 0.9503041 b-lin))]
    {:x (* x 100) :y (* y 100) :z (* z 100)}))

(defn xyz->rgb
  "Convert CIE XYZ to RGB"
  ([x y z] (xyz->rgb x y z 1))
  ([x y z a]
   (let [x' (/ x 100)
         y' (/ y 100)
         z' (/ z 100)
         ;; XYZ to linear RGB
         r-lin (+ (* 3.2404542 x') (* -1.5371385 y') (* -0.4985314 z'))
         g-lin (+ (* -0.9692660 x') (* 1.8760108 y') (* 0.0415560 z'))
         b-lin (+ (* 0.0556434 x') (* -0.2040259 y') (* 1.0572252 z'))
         ;; Gamma correction
         gamma (fn [v]
                 (if (<= v 0.0031308)
                   (* 12.92 v)
                   (- (* 1.055 (Math/pow v (/ 1 2.4))) 0.055)))]
     (rgb (Math/round (double (* (clamp (gamma r-lin) 0 1) 255)))
          (Math/round (double (* (clamp (gamma g-lin) 0 1) 255)))
          (Math/round (double (* (clamp (gamma b-lin) 0 1) 255)))
          (Math/round (double (* a 255)))))))

(defn xyz
  "Create color from CIE XYZ"
  ([x y z] (xyz x y z 1))
  ([x y z a] (xyz->rgb x y z a)))

(defn xyz->lab
  "Convert CIE XYZ to CIE LAB"
  [{:keys [x y z]}]
  (let [;; D65 reference white
        xn 95.047
        yn 100.0
        zn 108.883
        ;; Normalize
        x' (/ x xn)
        y' (/ y yn)
        z' (/ z zn)
        ;; f function
        f (fn [t]
            (if (> t 0.008856)
              (Math/pow t (/ 1 3))
              (+ (* 7.787 t) (/ 16 116))))
        fx (f x')
        fy (f y')
        fz (f z')
        ;; LAB
        l (- (* 116 fy) 16)
        a (* 500 (- fx fy))
        b (* 200 (- fy fz))]
    {:l l :a a :b b}))

(defn lab->xyz
  "Convert CIE LAB to XYZ"
  [{:keys [l a b]}]
  (let [;; D65 reference white
        xn 95.047
        yn 100.0
        zn 108.883
        ;; Compute
        fy (/ (+ l 16) 116)
        fx (+ (/ a 500) fy)
        fz (- fy (/ b 200))
        ;; Inverse f
        inv-f (fn [t]
                (let [t3 (* t t t)]
                  (if (> t3 0.008856)
                    t3
                    (/ (- t (/ 16 116)) 7.787))))
        x (* xn (inv-f fx))
        y (* yn (inv-f fy))
        z (* zn (inv-f fz))]
    {:x x :y y :z z}))

(defn lab->rgb
  "Convert CIE LAB to RGB"
  ([l a b] (lab->rgb l a b 1))
  ([l a b alpha]
   (let [{:keys [x y z]} (lab->xyz {:l l :a a :b b})]
     (xyz->rgb x y z alpha))))

(defn lab
  "Create color from CIE LAB. L: 0-100, a/b: -128 to 127"
  ([l a b] (lab l a b 1))
  ([l a b alpha] (lab->rgb l a b alpha)))

(defn rgb->lab
  "Convert RGB to CIE LAB"
  [color]
  (-> color rgb->xyz xyz->lab))

(defn lch->lab
  "Convert LCH to LAB"
  [{:keys [l c h]}]
  (let [h-rad (to-radians h)
        a (* c (Math/cos h-rad))
        b (* c (Math/sin h-rad))]
    {:l l :a a :b b}))

(defn lab->lch
  "Convert LAB to LCH"
  [{:keys [l a b]}]
  (let [c (Math/sqrt (+ (* a a) (* b b)))
        h (mod (to-degrees (Math/atan2 b a)) 360)]
    {:l l :c c :h h}))

(defn lch->rgb
  "Convert CIE LCH to RGB"
  ([l c h] (lch->rgb l c h 1))
  ([l c h alpha]
   (let [{:keys [l a b]} (lch->lab {:l l :c c :h h})]
     (lab->rgb l a b alpha))))

(defn lch
  "Create color from CIE LCH. L: 0-100, C: 0-~130, H: 0-360"
  ([l c h] (lch l c h 1))
  ([l c h a] (lch->rgb l c h a)))

(defn rgb->lch
  "Convert RGB to LCH"
  [color]
  (-> color rgb->lab lab->lch))

(defn hwb->rgb
  "Convert HWB (Hue-Whiteness-Blackness) to RGB"
  ([h w b] (hwb->rgb h w b 1))
  ([h w b a]
   (let [w' (if (> w 1) (/ w 100) w)
         b' (if (> b 1) (/ b 100) b)
         ;; Normalize if w + b > 1
         total (+ w' b')
         [w'' b''] (if (> total 1)
                     [(/ w' total) (/ b' total)]
                     [w' b'])
         ;; Get base hue color
         base (hsv->rgb h 1 1 a)
         ;; Apply whiteness and blackness
         factor (- 1 w'' b'')]
     (rgb (Math/round (double (+ (* (:r base) factor) (* 255 w''))))
          (Math/round (double (+ (* (:g base) factor) (* 255 w''))))
          (Math/round (double (+ (* (:b base) factor) (* 255 w''))))
          (:a base)))))

(defn hwb
  "Create color from HWB"
  ([h w b] (hwb h w b 1))
  ([h w b a] (hwb->rgb h w b a)))

;; ============================================================================
;; COLOR MANIPULATION
;; ============================================================================

(defn alpha
  "Set alpha channel of color"
  [color a]
  (assoc color :a (if (> a 1) a (Math/round (double (* a 255))))))

(defn lighten
  "Lighten color by factor (0-1)"
  [{:keys [r g b a]} factor]
  (let [f (+ 1 factor)]
    (rgb (min 255 (* r f))
         (min 255 (* g f))
         (min 255 (* b f))
         a)))

(defn darken
  "Darken color by factor (0-1)"
  [{:keys [r g b a]} factor]
  (let [f (- 1 factor)]
    (rgb (* r f) (* g f) (* b f) a)))

(defn mix
  "Mix two colors"
  ([c1 c2] (mix c1 c2 0.5))
  ([{r1 :r g1 :g b1 :b a1 :a} {r2 :r g2 :g b2 :b a2 :a} ratio]
   (let [r (- 1 ratio)]
     (rgb (+ (* r1 r) (* r2 ratio))
          (+ (* g1 r) (* g2 ratio))
          (+ (* b1 r) (* b2 ratio))
          (+ (* a1 r) (* a2 ratio))))))

(defn invert
  "Invert color"
  [{:keys [r g b a]}]
  (rgb (- 255 r) (- 255 g) (- 255 b) a))

(defn grayscale
  "Convert to grayscale"
  [{:keys [r g b a]}]
  (let [gray (Math/round (double (+ (* 0.299 r) (* 0.587 g) (* 0.114 b))))]
    (rgb gray gray gray a)))

;; ============================================================================
;; COLOR MODES
;; ============================================================================

(defn dark-mode
  "Transform color for dark mode"
  [color]
  ;; Invert lightness while preserving hue
  (let [{:keys [r g b a]} color
        ;; Convert to perceived brightness
        brightness (/ (+ (* 0.299 r) (* 0.587 g) (* 0.114 b)) 255)
        ;; Invert
        new-brightness (- 1 brightness)
        ;; Scale
        scale (if (zero? brightness) 1 (/ new-brightness brightness))]
    (rgb (min 255 (* r scale))
         (min 255 (* g scale))
         (min 255 (* b scale))
         a)))

(defn protanopia
  "Simulate protanopia (red-blind)"
  [{:keys [r g b a]}]
  (let [r' (/ r 255) g' (/ g 255) b' (/ b 255)]
    (rgb (Math/round (double (* 255 (+ (* 0.567 r') (* 0.433 g')))))
         (Math/round (double (* 255 (+ (* 0.558 r') (* 0.442 g')))))
         (Math/round (double (* 255 (+ (* 0.242 g') (* 0.758 b')))))
         a)))

(defn deuteranopia
  "Simulate deuteranopia (green-blind)"
  [{:keys [r g b a]}]
  (let [r' (/ r 255) g' (/ g 255) b' (/ b 255)]
    (rgb (Math/round (double (* 255 (+ (* 0.625 r') (* 0.375 g')))))
         (Math/round (double (* 255 (+ (* 0.7 r') (* 0.3 g')))))
         (Math/round (double (* 255 (+ (* 0.3 g') (* 0.7 b')))))
         a)))

(defn tritanopia
  "Simulate tritanopia (blue-blind)"
  [{:keys [r g b a]}]
  (let [r' (/ r 255) g' (/ g 255) b' (/ b 255)]
    (rgb (Math/round (double (* 255 (+ (* 0.95 r') (* 0.05 g')))))
         (Math/round (double (* 255 (+ (* 0.433 g') (* 0.567 b')))))
         (Math/round (double (* 255 (+ (* 0.475 g') (* 0.525 b')))))
         a)))

;; ============================================================================
;; TAILWIND COLORS (OKLCH)
;; ============================================================================

(def tw
  "Tailwind color palette"
  {;; Red
   :red-50 (oklch 0.971 0.013 17.38)
   :red-100 (oklch 0.936 0.032 17.717)
   :red-200 (oklch 0.885 0.062 18.334)
   :red-300 (oklch 0.808 0.114 19.571)
   :red-400 (oklch 0.704 0.191 22.216)
   :red-500 (oklch 0.637 0.237 25.331)
   :red-600 (oklch 0.577 0.245 27.325)
   :red-700 (oklch 0.505 0.213 27.518)
   :red-800 (oklch 0.444 0.177 26.899)
   :red-900 (oklch 0.396 0.141 25.723)
   :red-950 (oklch 0.258 0.092 26.042)

   ;; Orange
   :orange-50 (oklch 0.98 0.016 73.684)
   :orange-100 (oklch 0.954 0.038 75.164)
   :orange-200 (oklch 0.901 0.076 70.697)
   :orange-300 (oklch 0.837 0.128 66.29)
   :orange-400 (oklch 0.75 0.183 55.934)
   :orange-500 (oklch 0.705 0.213 47.604)
   :orange-600 (oklch 0.646 0.222 41.116)
   :orange-700 (oklch 0.553 0.195 38.402)
   :orange-800 (oklch 0.47 0.157 37.304)
   :orange-900 (oklch 0.408 0.123 38.172)
   :orange-950 (oklch 0.266 0.079 36.259)

   ;; Amber
   :amber-50 (oklch 0.987 0.022 95.277)
   :amber-100 (oklch 0.962 0.059 95.617)
   :amber-200 (oklch 0.924 0.12 95.746)
   :amber-300 (oklch 0.879 0.169 91.605)
   :amber-400 (oklch 0.828 0.189 84.429)
   :amber-500 (oklch 0.769 0.188 70.08)
   :amber-600 (oklch 0.666 0.179 58.318)
   :amber-700 (oklch 0.555 0.163 48.998)
   :amber-800 (oklch 0.473 0.137 46.201)
   :amber-900 (oklch 0.414 0.112 45.904)
   :amber-950 (oklch 0.279 0.077 45.635)

   ;; Yellow
   :yellow-50 (oklch 0.987 0.026 102.212)
   :yellow-100 (oklch 0.973 0.071 103.193)
   :yellow-200 (oklch 0.945 0.129 101.54)
   :yellow-300 (oklch 0.905 0.182 98.111)
   :yellow-400 (oklch 0.852 0.199 91.936)
   :yellow-500 (oklch 0.795 0.184 86.047)
   :yellow-600 (oklch 0.681 0.162 75.834)
   :yellow-700 (oklch 0.554 0.135 66.442)
   :yellow-800 (oklch 0.476 0.114 61.907)
   :yellow-900 (oklch 0.421 0.095 57.708)
   :yellow-950 (oklch 0.286 0.066 53.813)

   ;; Lime
   :lime-50 (oklch 0.986 0.031 120.757)
   :lime-100 (oklch 0.967 0.067 122.328)
   :lime-200 (oklch 0.938 0.127 124.321)
   :lime-300 (oklch 0.897 0.196 126.665)
   :lime-400 (oklch 0.841 0.238 128.85)
   :lime-500 (oklch 0.768 0.233 130.85)
   :lime-600 (oklch 0.648 0.2 131.684)
   :lime-700 (oklch 0.532 0.157 131.589)
   :lime-800 (oklch 0.453 0.124 130.933)
   :lime-900 (oklch 0.405 0.101 131.063)
   :lime-950 (oklch 0.274 0.072 132.109)

   ;; Green
   :green-50 (oklch 0.982 0.018 155.826)
   :green-100 (oklch 0.962 0.044 156.743)
   :green-200 (oklch 0.925 0.084 155.995)
   :green-300 (oklch 0.871 0.15 154.449)
   :green-400 (oklch 0.792 0.209 151.711)
   :green-500 (oklch 0.723 0.219 149.579)
   :green-600 (oklch 0.627 0.194 149.214)
   :green-700 (oklch 0.527 0.154 150.069)
   :green-800 (oklch 0.448 0.119 151.328)
   :green-900 (oklch 0.393 0.095 152.535)
   :green-950 (oklch 0.266 0.065 152.934)

   ;; Emerald
   :emerald-50 (oklch 0.979 0.021 166.113)
   :emerald-100 (oklch 0.95 0.052 163.051)
   :emerald-200 (oklch 0.905 0.093 164.15)
   :emerald-300 (oklch 0.845 0.143 164.978)
   :emerald-400 (oklch 0.765 0.177 163.223)
   :emerald-500 (oklch 0.696 0.17 162.48)
   :emerald-600 (oklch 0.596 0.145 163.225)
   :emerald-700 (oklch 0.508 0.118 165.612)
   :emerald-800 (oklch 0.432 0.095 166.913)
   :emerald-900 (oklch 0.378 0.077 168.94)
   :emerald-950 (oklch 0.262 0.051 172.552)

   ;; Teal
   :teal-50 (oklch 0.984 0.014 180.72)
   :teal-100 (oklch 0.953 0.051 180.801)
   :teal-200 (oklch 0.91 0.096 180.426)
   :teal-300 (oklch 0.855 0.138 181.071)
   :teal-400 (oklch 0.777 0.152 181.912)
   :teal-500 (oklch 0.704 0.14 182.503)
   :teal-600 (oklch 0.6 0.118 184.704)
   :teal-700 (oklch 0.511 0.096 186.391)
   :teal-800 (oklch 0.437 0.078 188.216)
   :teal-900 (oklch 0.386 0.063 188.416)
   :teal-950 (oklch 0.277 0.046 192.524)

   ;; Cyan
   :cyan-50 (oklch 0.984 0.019 200.873)
   :cyan-100 (oklch 0.956 0.045 203.388)
   :cyan-200 (oklch 0.917 0.08 205.041)
   :cyan-300 (oklch 0.865 0.127 207.078)
   :cyan-400 (oklch 0.789 0.154 211.53)
   :cyan-500 (oklch 0.715 0.143 215.221)
   :cyan-600 (oklch 0.609 0.126 221.723)
   :cyan-700 (oklch 0.52 0.105 223.128)
   :cyan-800 (oklch 0.45 0.085 224.283)
   :cyan-900 (oklch 0.398 0.07 227.392)
   :cyan-950 (oklch 0.302 0.056 229.695)

   ;; Sky
   :sky-50 (oklch 0.977 0.013 236.62)
   :sky-100 (oklch 0.951 0.026 236.824)
   :sky-200 (oklch 0.901 0.058 230.902)
   :sky-300 (oklch 0.828 0.111 230.318)
   :sky-400 (oklch 0.746 0.16 232.661)
   :sky-500 (oklch 0.685 0.169 237.323)
   :sky-600 (oklch 0.588 0.158 241.966)
   :sky-700 (oklch 0.5 0.134 242.749)
   :sky-800 (oklch 0.443 0.11 240.79)
   :sky-900 (oklch 0.391 0.09 240.876)
   :sky-950 (oklch 0.293 0.066 243.157)

   ;; Blue
   :blue-50 (oklch 0.97 0.014 254.604)
   :blue-100 (oklch 0.932 0.032 255.585)
   :blue-200 (oklch 0.882 0.059 254.128)
   :blue-300 (oklch 0.809 0.105 251.813)
   :blue-400 (oklch 0.707 0.165 254.624)
   :blue-500 (oklch 0.623 0.214 259.815)
   :blue-600 (oklch 0.546 0.245 262.881)
   :blue-700 (oklch 0.488 0.243 264.376)
   :blue-800 (oklch 0.424 0.199 265.638)
   :blue-900 (oklch 0.379 0.146 265.522)
   :blue-950 (oklch 0.282 0.091 267.935)

   ;; Indigo
   :indigo-50 (oklch 0.962 0.018 272.314)
   :indigo-100 (oklch 0.93 0.034 272.788)
   :indigo-200 (oklch 0.87 0.065 274.039)
   :indigo-300 (oklch 0.785 0.115 274.713)
   :indigo-400 (oklch 0.673 0.182 276.935)
   :indigo-500 (oklch 0.585 0.233 277.117)
   :indigo-600 (oklch 0.511 0.262 276.966)
   :indigo-700 (oklch 0.457 0.24 277.023)
   :indigo-800 (oklch 0.398 0.195 277.366)
   :indigo-900 (oklch 0.359 0.144 278.697)
   :indigo-950 (oklch 0.257 0.09 281.288)

   ;; Violet
   :violet-50 (oklch 0.969 0.016 293.756)
   :violet-100 (oklch 0.943 0.029 294.588)
   :violet-200 (oklch 0.894 0.057 293.283)
   :violet-300 (oklch 0.811 0.111 293.571)
   :violet-400 (oklch 0.702 0.183 293.541)
   :violet-500 (oklch 0.606 0.25 292.717)
   :violet-600 (oklch 0.541 0.281 293.009)
   :violet-700 (oklch 0.491 0.27 292.581)
   :violet-800 (oklch 0.432 0.232 292.759)
   :violet-900 (oklch 0.38 0.189 293.745)
   :violet-950 (oklch 0.283 0.141 291.089)

   ;; Purple
   :purple-50 (oklch 0.977 0.014 308.299)
   :purple-100 (oklch 0.946 0.033 307.174)
   :purple-200 (oklch 0.902 0.063 306.703)
   :purple-300 (oklch 0.827 0.119 306.383)
   :purple-400 (oklch 0.714 0.203 305.504)
   :purple-500 (oklch 0.627 0.265 303.9)
   :purple-600 (oklch 0.558 0.288 302.321)
   :purple-700 (oklch 0.496 0.265 301.924)
   :purple-800 (oklch 0.438 0.218 303.724)
   :purple-900 (oklch 0.381 0.176 304.987)
   :purple-950 (oklch 0.291 0.149 302.717)

   ;; Fuchsia
   :fuchsia-50 (oklch 0.977 0.017 320.058)
   :fuchsia-100 (oklch 0.952 0.037 318.852)
   :fuchsia-200 (oklch 0.903 0.076 319.62)
   :fuchsia-300 (oklch 0.833 0.145 321.434)
   :fuchsia-400 (oklch 0.74 0.238 322.16)
   :fuchsia-500 (oklch 0.667 0.295 322.15)
   :fuchsia-600 (oklch 0.591 0.293 322.896)
   :fuchsia-700 (oklch 0.518 0.253 323.949)
   :fuchsia-800 (oklch 0.452 0.211 324.591)
   :fuchsia-900 (oklch 0.401 0.17 325.612)
   :fuchsia-950 (oklch 0.293 0.136 325.661)

   ;; Pink
   :pink-50 (oklch 0.971 0.014 343.198)
   :pink-100 (oklch 0.948 0.028 342.258)
   :pink-200 (oklch 0.899 0.061 343.231)
   :pink-300 (oklch 0.823 0.12 346.018)
   :pink-400 (oklch 0.718 0.202 349.761)
   :pink-500 (oklch 0.656 0.241 354.308)
   :pink-600 (oklch 0.592 0.249 0.584)
   :pink-700 (oklch 0.525 0.223 3.958)
   :pink-800 (oklch 0.459 0.187 3.815)
   :pink-900 (oklch 0.408 0.153 2.432)
   :pink-950 (oklch 0.284 0.109 3.907)

   ;; Rose
   :rose-50 (oklch 0.969 0.015 12.422)
   :rose-100 (oklch 0.941 0.03 12.58)
   :rose-200 (oklch 0.892 0.058 10.001)
   :rose-300 (oklch 0.81 0.117 11.638)
   :rose-400 (oklch 0.712 0.194 13.428)
   :rose-500 (oklch 0.645 0.246 16.439)
   :rose-600 (oklch 0.586 0.253 17.585)
   :rose-700 (oklch 0.514 0.222 16.935)
   :rose-800 (oklch 0.455 0.188 13.697)
   :rose-900 (oklch 0.41 0.159 10.272)
   :rose-950 (oklch 0.271 0.105 12.094)

   ;; Slate
   :slate-50 (oklch 0.984 0.003 247.858)
   :slate-100 (oklch 0.968 0.007 247.896)
   :slate-200 (oklch 0.929 0.013 255.508)
   :slate-300 (oklch 0.869 0.022 252.894)
   :slate-400 (oklch 0.704 0.04 256.788)
   :slate-500 (oklch 0.554 0.046 257.417)
   :slate-600 (oklch 0.446 0.043 257.281)
   :slate-700 (oklch 0.372 0.044 257.287)
   :slate-800 (oklch 0.279 0.041 260.031)
   :slate-900 (oklch 0.208 0.042 265.755)
   :slate-950 (oklch 0.129 0.042 264.695)

   ;; Gray
   :gray-50 (oklch 0.985 0.002 247.839)
   :gray-100 (oklch 0.967 0.003 264.542)
   :gray-200 (oklch 0.928 0.006 264.531)
   :gray-300 (oklch 0.872 0.01 258.338)
   :gray-400 (oklch 0.707 0.022 261.325)
   :gray-500 (oklch 0.551 0.027 264.364)
   :gray-600 (oklch 0.446 0.03 256.802)
   :gray-700 (oklch 0.373 0.034 259.733)
   :gray-800 (oklch 0.278 0.033 256.848)
   :gray-900 (oklch 0.21 0.034 264.665)
   :gray-950 (oklch 0.13 0.028 261.692)

   ;; Zinc
   :zinc-50 (oklch 0.985 0 0)
   :zinc-100 (oklch 0.967 0.001 286.375)
   :zinc-200 (oklch 0.92 0.004 286.32)
   :zinc-300 (oklch 0.871 0.006 286.286)
   :zinc-400 (oklch 0.705 0.015 286.067)
   :zinc-500 (oklch 0.552 0.016 285.938)
   :zinc-600 (oklch 0.442 0.017 285.786)
   :zinc-700 (oklch 0.37 0.013 285.805)
   :zinc-800 (oklch 0.274 0.006 286.033)
   :zinc-900 (oklch 0.21 0.006 285.885)
   :zinc-950 (oklch 0.141 0.005 285.823)

   ;; Neutral
   :neutral-50 (oklch 0.985 0 0)
   :neutral-100 (oklch 0.97 0 0)
   :neutral-200 (oklch 0.922 0 0)
   :neutral-300 (oklch 0.87 0 0)
   :neutral-400 (oklch 0.708 0 0)
   :neutral-500 (oklch 0.556 0 0)
   :neutral-600 (oklch 0.439 0 0)
   :neutral-700 (oklch 0.371 0 0)
   :neutral-800 (oklch 0.269 0 0)
   :neutral-900 (oklch 0.205 0 0)
   :neutral-950 (oklch 0.145 0 0)

   ;; Stone
   :stone-50 (oklch 0.985 0.001 106.423)
   :stone-100 (oklch 0.97 0.001 106.424)
   :stone-200 (oklch 0.923 0.003 48.717)
   :stone-300 (oklch 0.869 0.005 56.366)
   :stone-400 (oklch 0.709 0.01 56.259)
   :stone-500 (oklch 0.553 0.013 58.071)
   :stone-600 (oklch 0.444 0.011 73.639)
   :stone-700 (oklch 0.374 0.01 67.558)
   :stone-800 (oklch 0.268 0.007 34.298)
   :stone-900 (oklch 0.216 0.006 56.043)
   :stone-950 (oklch 0.147 0.004 49.25)

   ;; Black/White
   :black (rgb 0 0 0)
   :white (rgb 255 255 255)})

;; ============================================================================
;; HTML NAMED COLORS
;; ============================================================================

(def html
  "HTML/CSS named colors"
  {;; Reds
   :indian-red (rgb 205 92 92)
   :light-coral (rgb 240 128 128)
   :salmon (rgb 250 128 114)
   :dark-salmon (rgb 233 150 122)
   :light-salmon (rgb 255 160 122)
   :crimson (rgb 220 20 60)
   :red (rgb 255 0 0)
   :fire-brick (rgb 178 34 34)
   :dark-red (rgb 139 0 0)

   ;; Pinks
   :pink (rgb 255 192 203)
   :light-pink (rgb 255 182 193)
   :hot-pink (rgb 255 105 180)
   :deep-pink (rgb 255 20 147)
   :medium-violet-red (rgb 199 21 133)
   :pale-violet-red (rgb 219 112 147)

   ;; Oranges
   :coral (rgb 255 127 80)
   :tomato (rgb 255 99 71)
   :orange-red (rgb 255 69 0)
   :dark-orange (rgb 255 140 0)
   :orange (rgb 255 165 0)

   ;; Yellows
   :gold (rgb 255 215 0)
   :yellow (rgb 255 255 0)
   :light-yellow (rgb 255 255 224)
   :lemon-chiffon (rgb 255 250 205)
   :light-goldenrod-yellow (rgb 250 250 210)
   :papaya-whip (rgb 255 239 213)
   :moccasin (rgb 255 228 181)
   :peach-puff (rgb 255 218 185)
   :pale-goldenrod (rgb 238 232 170)
   :khaki (rgb 240 230 140)
   :dark-khaki (rgb 189 183 107)

   ;; Purples
   :lavender (rgb 230 230 250)
   :thistle (rgb 216 191 216)
   :plum (rgb 221 160 221)
   :violet (rgb 238 130 238)
   :orchid (rgb 218 112 214)
   :fuchsia (rgb 255 0 255)
   :magenta (rgb 255 0 255)
   :medium-orchid (rgb 186 85 211)
   :medium-purple (rgb 147 112 219)
   :rebecca-purple (rgb 102 51 153)
   :blue-violet (rgb 138 43 226)
   :dark-violet (rgb 148 0 211)
   :dark-orchid (rgb 153 50 204)
   :dark-magenta (rgb 139 0 139)
   :purple (rgb 128 0 128)
   :indigo (rgb 75 0 130)
   :slate-blue (rgb 106 90 205)
   :dark-slate-blue (rgb 72 61 139)
   :medium-slate-blue (rgb 123 104 238)

   ;; Greens
   :green-yellow (rgb 173 255 47)
   :chartreuse (rgb 127 255 0)
   :lawn-green (rgb 124 252 0)
   :lime (rgb 0 255 0)
   :lime-green (rgb 50 205 50)
   :pale-green (rgb 152 251 152)
   :light-green (rgb 144 238 144)
   :medium-spring-green (rgb 0 250 154)
   :spring-green (rgb 0 255 127)
   :medium-sea-green (rgb 60 179 113)
   :sea-green (rgb 46 139 87)
   :forest-green (rgb 34 139 34)
   :green (rgb 0 128 0)
   :dark-green (rgb 0 100 0)
   :yellow-green (rgb 154 205 50)
   :olive-drab (rgb 107 142 35)
   :olive (rgb 128 128 0)
   :dark-olive-green (rgb 85 107 47)
   :medium-aquamarine (rgb 102 205 170)
   :dark-sea-green (rgb 143 188 139)
   :light-sea-green (rgb 32 178 170)
   :dark-cyan (rgb 0 139 139)
   :teal (rgb 0 128 128)

   ;; Blues
   :aqua (rgb 0 255 255)
   :cyan (rgb 0 255 255)
   :light-cyan (rgb 224 255 255)
   :pale-turquoise (rgb 175 238 238)
   :aquamarine (rgb 127 255 212)
   :turquoise (rgb 64 224 208)
   :medium-turquoise (rgb 72 209 204)
   :dark-turquoise (rgb 0 206 209)
   :cadet-blue (rgb 95 158 160)
   :steel-blue (rgb 70 130 180)
   :light-steel-blue (rgb 176 196 222)
   :powder-blue (rgb 176 224 230)
   :light-blue (rgb 173 216 230)
   :sky-blue (rgb 135 206 235)
   :light-sky-blue (rgb 135 206 250)
   :deep-sky-blue (rgb 0 191 255)
   :dodger-blue (rgb 30 144 255)
   :cornflower-blue (rgb 100 149 237)
   :royal-blue (rgb 65 105 225)
   :blue (rgb 0 0 255)
   :medium-blue (rgb 0 0 205)
   :dark-blue (rgb 0 0 139)
   :navy (rgb 0 0 128)
   :midnight-blue (rgb 25 25 112)

   ;; Browns
   :cornsilk (rgb 255 248 220)
   :blanched-almond (rgb 255 235 205)
   :bisque (rgb 255 228 196)
   :navajo-white (rgb 255 222 173)
   :wheat (rgb 245 222 179)
   :burly-wood (rgb 222 184 135)
   :tan (rgb 210 180 140)
   :rosy-brown (rgb 188 143 143)
   :sandy-brown (rgb 244 164 96)
   :goldenrod (rgb 218 165 32)
   :dark-goldenrod (rgb 184 134 11)
   :peru (rgb 205 133 63)
   :chocolate (rgb 210 105 30)
   :saddle-brown (rgb 139 69 19)
   :sienna (rgb 160 82 45)
   :brown (rgb 165 42 42)
   :maroon (rgb 128 0 0)

   ;; Whites
   :white (rgb 255 255 255)
   :snow (rgb 255 250 250)
   :honeydew (rgb 240 255 240)
   :mint-cream (rgb 245 255 250)
   :azure (rgb 240 255 255)
   :alice-blue (rgb 240 248 255)
   :ghost-white (rgb 248 248 255)
   :white-smoke (rgb 245 245 245)
   :seashell (rgb 255 245 238)
   :beige (rgb 245 245 220)
   :old-lace (rgb 253 245 230)
   :floral-white (rgb 255 250 240)
   :ivory (rgb 255 255 240)
   :antique-white (rgb 250 235 215)
   :linen (rgb 250 240 230)
   :lavender-blush (rgb 255 240 245)
   :misty-rose (rgb 255 228 225)

   ;; Grays
   :gainsboro (rgb 220 220 220)
   :light-gray (rgb 211 211 211)
   :silver (rgb 192 192 192)
   :dark-gray (rgb 169 169 169)
   :gray (rgb 128 128 128)
   :dim-gray (rgb 105 105 105)
   :light-slate-gray (rgb 119 136 153)
   :slate-gray (rgb 112 128 144)
   :dark-slate-gray (rgb 47 79 79)
   :black (rgb 0 0 0)})

;; ============================================================================
;; COLOR RESOLVER
;; ============================================================================

(defn resolve-color
  "Resolve color from various formats"
  [c]
  (cond
    ;; Already resolved
    (and (map? c) (:r c)) c

    ;; Keyword - check tw then html
    (keyword? c)
    (or (get tw c)
        (get html c)
        (throw (ex-info (str "Unknown color: " c) {:color c})))

    ;; Vector RGB/RGBA
    (vector? c)
    (case (count c)
      3 (apply rgb c)
      4 (apply rgba c)
      (throw (ex-info "Invalid color vector" {:color c})))

    ;; String - hex
    (string? c)
    (hex->rgb c)

    ;; Function - defer resolution
    (fn? c) c

    :else
    (throw (ex-info "Invalid color format" {:color c}))))

;; ============================================================================
;; TESTS
;; ============================================================================

#?(:clj
   (tests
    "RGB creation"
    (rgb 255 0 0) := {:r 255 :g 0 :b 0 :a 255}

    "RGBA creation"
    (rgba 255 0 0 128) := {:r 255 :g 0 :b 0 :a 128}

    "HSL to RGB"
    (:r (hsl 0 100 50)) := 255
    (:g (hsl 0 100 50)) := 0
    (:b (hsl 0 100 50)) := 0

    "Hex to RGB"
    (hex "#FF0000") := {:r 255 :g 0 :b 0 :a 255}
    (hex "#F00") := {:r 255 :g 0 :b 0 :a 255}

    "Color manipulation"
    (:a (alpha (rgb 255 0 0) 0.5)) := 128

    "Tailwind colors exist"
    (some? (:red-500 tw)) := true
    (some? (:blue-500 tw)) := true

    "HTML colors exist"
    (some? (:coral html)) := true
    (some? (:navy html)) := true

    "Resolve color - keyword"
    (resolve-color :red-500) := (:red-500 tw)

    "Resolve color - vector"
    (resolve-color [255 0 0]) := {:r 255 :g 0 :b 0 :a 255}

    "Resolve color - string"
    (resolve-color "#FF0000") := {:r 255 :g 0 :b 0 :a 255}))
