(step
  (if
    (inState search)
    (if
      (lt (getRotations) 0.25)
      (setSpeed 0.1 -0.1)
      (and (setSpeed 0.0 0.0) (setState backup))))
  (if
    (inState backup)
    (if
      (gt (getRange 8) 12)
      (setSpeed -0.05 -0.05)
      (and (setSpeed 0.0 0.0) (setState uturn))))
  (if
    (inState uturn)
    (if
      (lt (getRotations) 0.5)
      (setSpeed 0.1 -0.1)
      (and (setSpeed 0.0 0.0) (setState backup))))
  (or
    (if (lt (getRange 0) 6) (setSpeed -0.2 -0.2))
    (if (lt (getRange 4) 6) (setSpeed -0.1 0.2))
    (if (lt (getRange 3) 6) (setSpeed -0.1 0.225))
    (if (lt (getRange 2) 6) (setSpeed -0.1 0.25))
    (if (lt (getRange 1) 6) (setSpeed -0.1 0.275))
    (if (lt (getRange 15) 6) (setSpeed 0.275 -0.1))
    (if (lt (getRange 14) 6) (setSpeed 0.25 -0.1))
    (if (lt (getRange 13) 6) (setSpeed 0.225 -0.1))
    (if (lt (getRange 12) 6) (setSpeed 0.2 -0.1))))
