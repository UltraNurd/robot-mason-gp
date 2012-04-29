(step
  (if
    (inState search)
    (if
      (lt (getMidpoint) 15)
      (setSpeed -0.1 0.1)
      (if
        (gt (getMidpoint) 15)
        (setSpeed 0.1 -0.1)
        (if
          (lt (getWidth) 13)
          (setSpeed 0.2 0.2)
          (and (setSpeed 0.0 0.0) (pickUp))))))
  (if
    (inState backup)
    (if
      (and (gt (getTravel) -12) (gt (getRange 8) 2))
      (setSpeed -0.2 -0.2)
      (and (setSpeed 0.0 0.0) (setState uturn))))
  (if
    (inState uturn)
    (if
      (lt (getRotations) 0.5)
      (setSpeed 0.1 -0.1)
      (and (setSpeed 0.0 0.0) (setState search))))
  (if
    (inState carry)
    (or
      (if
        (lt (getMidpoint) 15)
        (setSpeed -0.1 0.1))
      (if
        (gt (getMidpoint) 15)
        (setSpeed 0.1 -0.1))
      (if
        (lt (getWidth) 15)
        (setSpeed 0.2 0.2)
        (and (setSpeed 0.0 0.0) (drop)))))
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
