(step
  (if
    (not
      (or
        (if (lt (getRange 0) 6) (setSpeed -0.2 -0.2))
        (if (lt (getRange 4) 6) (setSpeed -0.1 0.4))
        (if (lt (getRange 3) 6) (setSpeed -0.1 0.425))
        (if (lt (getRange 2) 6) (setSpeed -0.1 0.45))
        (if (lt (getRange 1) 6) (setSpeed -0.1 0.475))
        (if (lt (getRange 15) 6) (setSpeed 0.475 -0.1))
        (if (lt (getRange 14) 6) (setSpeed 0.45 -0.1))
        (if (lt (getRange 13) 6) (setSpeed 0.425 -0.1))
        (if (lt (getRange 12) 6) (setSpeed 0.4 -0.1))))
    (if
      (or (and (not (inState carry)) (lt (getMidpoint) 15))
          (and (inState carry) (lt (getMidpoint) 8)))
      (setSpeed -0.1 0.1)
      (if
        (or (and (not (inState carry)) (gt (getMidpoint) 15))
            (and (inState carry) (gt (getMidpoint) 22)))
        (setSpeed 0.1 -0.1)
        (if
          (or (inState carry) (lt (getWidth) 13))
          (setSpeed 0.2 0.2)
          (and (setSpeed 0.0 0.0) (pickUp)))))))
