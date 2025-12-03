(ns foton.replicant
  "Replicant alias registration for Foton primitives and composites.

   Simplified: direct function references, no multimethod dispatch."
  (:require [replicant.alias :as alias]
            [foton.primitives :as p]
            [foton.composites :as c]))

(defn init!
  "Initialize Foton primitives and composites as Replicant aliases.

   Primitives: :foton.css/* (e.g., :foton.css/frame, :foton.css/text)
   Composites: :foton/* (e.g., :foton/button, :foton/fade-in)

   Usage:
     (foton/init!)

   After init, use in views:
     [:foton.css/frame {:fill :card}
       [:foton.css/text {:preset :heading} \"Hello\"]]

     [:foton/button {:variant :primary :on {:click [...]}}
       [:foton.css/text {:color :white} \"Click me\"]]"
  []
  ;; Primitives - :foton.css/*
  (alias/register! :foton.css/frame p/render-frame)
  (alias/register! :foton.css/text p/render-text)
  (alias/register! :foton.css/icon p/render-icon)
  (alias/register! :foton.css/input p/render-input)
  (alias/register! :foton.css/textarea p/render-textarea)
  (alias/register! :foton.css/link p/render-link)
  (alias/register! :foton.css/image p/render-image)
  (alias/register! :foton.css/video p/render-video)
  (alias/register! :foton.css/svg p/render-svg)

  ;; Composites - :foton/*
  (alias/register! :foton/button c/render-button)
  (alias/register! :foton/fade-in c/render-fade-in)
  (alias/register! :foton/fade-out c/render-fade-out)
  (alias/register! :foton/slide-up c/render-slide-up)
  (alias/register! :foton/slide-down c/render-slide-down)
  (alias/register! :foton/slide-left c/render-slide-left)
  (alias/register! :foton/slide-right c/render-slide-right)
  (alias/register! :foton/scale-in c/render-scale-in)
  (alias/register! :foton/scale-out c/render-scale-out)

  ;; Interactive effects
  (alias/register! :foton/lift c/render-lift)
  (alias/register! :foton/sink c/render-sink)
  (alias/register! :foton/grow c/render-grow)
  (alias/register! :foton/shrink c/render-shrink)
  (alias/register! :foton/tilt c/render-tilt)
  (alias/register! :foton/glow c/render-glow)
  (alias/register! :foton/pop c/render-pop)
  (alias/register! :foton/press c/render-press)

  ;; Drag & resize
  (alias/register! :foton/draggable c/render-draggable)
  (alias/register! :foton/resizable c/render-resizable))
