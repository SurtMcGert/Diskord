package com.chat.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.datatransfer.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Client extends JPanel implements KeyListener, ActionListener {
    JFrame frame;
    int textFiedBoundry = 0;
    int currentTextEndBoundry = 0;
    boolean textSelected = false;
    ArrayList<String> previousText = new ArrayList<>();
    String currentText = "";
    String inProgressText = "";
    Font font = new Font("Impact", Font.TRUETYPE_FONT, 40);// sets the normal font
    int sub = previousText.size() - 1;// the index of the message the chat history is to start from
    boolean ctrl = false;
    boolean alt = false;
    boolean delete = false;
    String userName = null;
    int localNumOfSavedMessages = 50; // the number of localy loaded messages in the chat
    String configFile = "config.txt";
    String serverDetailsFile = "serverDetails.txt";
    int lastOffsetInBytes = 0;// the number of bytes that were ignored in the last download of messages
    String oldestLoadedChunk = "";
    int numOfExtraLoadedChunks = 0;
    String fontSizeRegex = "^!(fontSize)\\((\\d*)\\)$";
    String colourRegex = "^!(\\w*)Colour\\(((\\d+),(\\d+),(\\d+))\\)$";
    String serverInfoRegex = "^!serverInfo$";
    String setServerInfoRegex = "^!set(\\w*)\\(((?:\\d+.\\d+.\\d+.\\d+)||(?:\\d+))\\)$";
    String reConnectRegex = "^!connect$";
    String connectedUsersRegex = "^!connectedClients$";
    int cursorOffset = 0;
    Config cfg;
    InputStream in = null;
    BufferedReader bin = null;
    Socket sock = null;
    PrintWriter writer = null;
    ClientThread ct = null;
    protected boolean connectedToServer = false;
    protected boolean updateSuccess = false;
    private ArrayList<String> personalMessageLog = new ArrayList<String>();
    private int personalMessageLogIndex = 0;
    private String icon = "iVBORw0KGgoAAAANSUhEUgAAAOIAAAEACAYAAACu66rqAAALo3pUWHRSYXcgcHJvZmlsZSB0eXBlIGV4aWYAAHja7Zhrkhy5DYT/8xQ+Al8gieOQBBnhG/j4/lDdGo200kob6z92eDqmq4dVwweQyEx0OP/65w3/4KdIjaFKH01bi/xUrZonH0Z8/ejznmJ93p+fk9/30rfj4eNj5lq4lteNPt/PT8bl6z98WSOtb8fDeN/J4z3R+0Z8r1x8Zf9snzfJeH6Np/qeSM/rQ9PRP291vSfa7wefrbx/68e2Xhf/O3wz0ImSCQuVnE9JJT7v9bWD4r+pTK6d91RS/jJSSwzPjfaejIB8c7wv1xg/B+iHwQ/fR/9nwc/zPV6+i2X7MlH78Y0k342Xj2Xy54XL+1Ng+JsbBH//4Tjv33tt3Htep5u1EdH2RtQHjp5peHAR8vL8W+PV+RU+9+elvEaccZNyY7nFaydNmazckGqyNNNN57nutNlizSd3rjlv0uJjo/SseZMYsuivdHMvWqwMMrfzCaUwnD/2kp519Vlvp8HKlng0JyZL/MtPX+HPbv6VV7jXY5tSHK84gQv2lR0dbMMz5+88RULSfedNngB/eX0UbfyU2EIG5Qnz4IAzrtcUS9JXbJUnz4XnhOurhFLo9p6AELG2sBnAX1NsqUhqKface0rEcZCgyc5zqXmRgSSSjU3mWkrLoeeRfW3+p6fn2Sy5ZR+Gm0iElEZtDTI0SVatAn56HWBoCowmIk26jCAqs5VWm7TWenOSm7302qW33vvo2ucoow4ZbfQxho6pWQscKNq061DVOXOYLDSZa/L8ZGTlVVZdstrqayxdcwOfXbfstvseW/e0bMWgCWvWbZjaPCkcmOLUI6edfsbRMy9Yu+XWK7fdfsfVOz+ylt5l+/3rL2QtvbOWn0z5c/0ja4yG3r9MkZxOxHNGxnJNZLx7BgB09pzFkWrNnjnPWdRMUUhmk+K5CZY8Y6SwnpTlpo/cfc3cb+UtyPitvOVfZS546v4TmQuk7o95+0HWzHVuPxl7VaHHNJbr1XkGOBrTNW3mv3v9/0T/lRNpjdnLsuR6m3sUxXo1Q5wuGJ+7Ua9Tdj0zg0G0piaQvuJtgGhaRye137hDrw5RlSNj2y6AkYqfmCGMHGBX7daB5MoyKyJGydfdqJbkuEd0qU5KYGsoyDAFOKx3qv+WcafA4zxF/S1UeWU7OKCdzlrSk85FVVe4KbeVlcqfyGbtIVbJ0qdRVhfhP1eidYT25H2OH3F1jedg9crup+a5bF2Eeq1brB8rUzeSvMPdtlLVQkQqlrBrsyRna6eMGtdpNjjFRF22jRPXPLWxl3JPqvtMsSV5yQ1bWQADWUayDHcss3ZuYYoGN6k6F7VSxsmrUsubVYhiQonyUcurxVMuT8CQN08KHaa7CBMbS7nrPDaL8ug+Jr2evQSawOr6prdeWCVvbMg4bdgpPe0AUaaRs6oQHU5w0mUdqfDaWguEVLsGgZ22xjBZB2Ec9ca1ml5sD9G6wz3k1ca5tkBphoCQdWGaG49sHcfRcoTsQ7ns3AjMHdayuS+W5FF9X8P3Az+/Eta+gUfczpwsazBkIojadk2hEceot86Dtx1yPPFlcEohXWgHGFhHEYtxBl5gYHxrauR8aVpqYxwOj/NEIOcQidOZFOBGNwa/tcH3FQEZo9caRpFj6ThKsZai/Ui7WIdbmotBryC9TRRMZdMGKLIn5VyAe1GirrVvWYQ0JBTqylzM2xAsQ2nG4VTKFm8u86nwGH95Dd8OFCNLqd3RXfqsee5xms2xy45xktx05aDsiFAH4HyuLc1Q75CJWUqcpXPEcrod6uCv0lLwDxMtW1SEB6XJFt17Js1SaWQ6CV36lOFeaU7xfNtUA3ou4wOxvDnPwP5mIWDb8qAKFhaD8WgIffIe64za/TDc2tTDT6MU/nhj6RTRYaSuIdyl6K6WJ2a+y05lzNnFqSTvFSmjm9gGNKJUBe4gUYh0n0h79JIxo2pzirYqfIdZnyN9RphUj/a+Z1OZ+cxzwz3d8TP5b6XWWgfuC2tme0DRqQAy4Jb2JJCpk63da7lJ22WedHrTBYmJhdtoNqzrpvI7xME71GdwltipHWHAnphTa91YHDwJGnGyLBwZVHPSjlepqmAOF7O+TzqRwib7HLrCiRRkbgVAM10+7m3l8pFEVqjHImJBxg0+HHPXQKzabDSzVLWxCyq2aszYdWKm9LNp3z70ntwem9WjUe4LGtI15C4bBX7RG8jNNtO7myyFSrutUm2sTQCPcOxLXA6lVyd3N9w4AJptzZy5ObwMfUstLGuwcELbMg+Ru0kZbpDJKiwy4UaQFfeF6HolvjQDDRKpnATZQCQM8Rk+0f0EM1ByOQems01BJoD3uGlB3LtiLK3CSjXeDTojPDW7CT0mVZdDg8r6XYS2PDKgiJovxq5mWUXTI7u42F/QU/gg0sNZYmuxNmat5Ot2dUUlQB2h64/MjQ1FAdaj7WQ6YyTcHtKo4aOEqYXNicgRyDvrwlDQ19q5xome1vz0aWMPtM25SnGwlYYMFK8uEtBuFpuYXDQCB3CSgs8CNuFF6ht6weTGVrAWg+bFCu0u2QEHA2m7uR62BI2giAjCU3wJO0Hjpw1lzZfuOzZmJ700AlqHolL+PQ9++pkM10AlDSKB/q2Q5ZR4XCMmCFhLn2NSSgPCEySx07FT1vjqPlbKNAkGVwtgUxiMJ5QqgY/ck7zPTJnOOniuJ5dW5QQb/vSgi21cySTzdJ2dQNLxEwDcPcBYu5wYMkeqN3OoBYnga/oSd/RIqktzX2x4oWdSugt+ZP07yeWtAmWDVhQT8nEVWQiPxkHln3oXhaFnZyDwiZVrj3RMIyWNnnPQNu6mhYPqQSFgKTXgMuaipGKyiaSi/0r/0p1UNt2ajTSQaCc7eps/0aTwzcDteITqHgcThjG8Vwyvh7VcHhH4gRQiVTjDBitRiXGTo8My8NH5pCqI4ySdRhliBi/0ZzSkFfdXgI6TindiG2/KxBX+I/xuMquEnGgpQToeL+7zOKkf1tN4ii/vTYJ9xrigN+S+jwmvL3x2ggVJHRyjuA+ptG64WdxUmeNFhL5d+Xr8J4b6HB/bci7kRg4DtUBDp0fdNlysouCIN6k9Eg+d9H4F+vxK/MNXOVuzHa+IWWUPOlDsD6YdLUebRmcmSM7pAKg99kANyZ+ZDN+pKbiBn2VDEXdPyAK7jtvMuQ5srn8HpQ6exIko4J04J5DfALaon4iKcAM6JYyomEVzpnbJaV5IqVmkKFAIiLEuiMdMoH78qJrXPpWJ/Ksw61j0Fsd2gD9Y6KI5bhJlQY0PRWDcD+GCCOwFaj8UVJUFz4OjdV+PFroAXGSgY9hxx3cJxhcnZl6E2Gu0NLvmTqmzVnVhw1zSl4hgATyVtFDpUqWYEjqmWpAjYBAbGyLY+CbMB084u3eXcQqTBIKfxsGX0HhRkO1mBETmbR2RR2zWqAGpQrIbexmVcfXvyV/JueZYAXjEFtjAG49XI+pgrrsLuXVALvRpy+Aj63ENurUUEUJQ5Jo3iT6SWaG+eS9XxNwbn0zZd6z2ptfqlZYKaGMozmo5EBFW6AvRHrD+8+X6irsjwBVu9W9HigBgZ8VYyOBKBU/cpNC5OOZhJ2j5BmRewX9J2Fk8i3mDlzEeNDygHUVM/oKEkcVM5iMqUvvMFX1APmikEu2hePVD/CbTsLHXSkEKc/Nw5gEk1XUhG7KJY43u24obzouhquILo3gHa5lvYLxij8C3EL6TaTES6W97Qz4RzY4YINjl0h9BTXQhoN/gJdwoOIToN0R7Tugo1/AAN5zL9QrqlH3CPeJ8kJRB/sAcjJrgcWgDRGHkV1kweXPmqJQGVGu/Z+x/drW06IGXxwiH/7RM22WQRffDTY4CjuwChvhzAlgYjnRPQUhwXN/PG75pIPybNmveaFP01uizHVpQSIQwFkJOTw+BqVuI1l2ZMMbAIxpuBPZ7uPRvXv8/0f/IROneaxr+DQJ285EG92bKAAABhWlDQ1BJQ0MgcHJvZmlsZQAAeJx9kT1Iw0AcxV9TpaIVh3YQUchQnSyIioiTVLEIFkpboVUHk0s/hCYNSYqLo+BacPBjserg4qyrg6sgCH6AuLk5KbpIif9LCi1iPDjux7t7j7t3gFAvM9XsGANUzTJS8ZiYza2IgVcE0IMQhjAjMVNPpBcy8Bxf9/Dx9S7Ks7zP/Tl6lbzJAJ9IPMt0wyJeJ57atHTO+8RhVpIU4nPiUYMuSPzIddnlN85FhwWeGTYyqTniMLFYbGO5jVnJUIkniSOKqlG+kHVZ4bzFWS1XWfOe/IXBvLac5jrNQcSxiASSECGjig2UYSFKq0aKiRTtxzz8A44/SS6ZXBtg5JhHBSokxw/+B7+7NQsT425SMAZ0vtj2xzAQ2AUaNdv+PrbtxgngfwautJa/UgemP0mvtbTIEdC3DVxctzR5D7jcAfqfdMmQHMlPUygUgPcz+qYcELoFulfd3pr7OH0AMtTV0g1wcAiMFCl7zePdXe29/Xum2d8PwkNyx12ZgQEAAAAGYktHRAD/AP8A/6C9p5MAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQflBwsUKAc3akslAAAgAElEQVR42u29aXMbaXYu+Ly5AonEDgIEd4kSqY2Spdq7u0rd7uvu2+72nZmYsGfixr1xv/gfzO+YfzAf7PCX6RvXc8fhGXu67W73UtVVZclSSVUlUdRGUuC+AMSORGa+8+E92USxtXABCIDMJwIBrWAi8zzv2c9hP/nJT+DDh4/uQvJvgQ8fPhF9+PDhE9GHj96A4t+CnkQKwACAMAAdgAzABWADaACoA6jRe53+rEl/77YcsgoAlT4jQK8gvev09xIAhz6jBGADwKb/CHwinhbEAIzSu0SEygNYJiIclQwuAItelQP+XxPAEIA4kdYFUADwgt59+ETsS2QAjAMwSJOtAFjsYaEuA5h7xd+NAciSZq0CWACw5j9in4i9iPMABgFw0nDPTpCwLtKrFWdJgzIAqwAe+yLgE7EbGAEwQcSbJ0E8TcL4jF4ehul+MLofOV9EfCJ2ClcBJCCCG1/7wvYNLNHLw2WI4NM2gPv+7fGJeFS8BSAE4LkvUAfC1y2/HgVwBiKA9G/+rfGJuF9cg4hsPvMFpy14QS+PlGchglb3fCL62IsxEpDV0y4gx0jKCxABrmf4w2CQT8RThvcgcnqfdlkYJOwm5L2kfOu79/L+HaP/xyFyfl7y33s197zbLf+uVzBLLwD4gK7tc5+IpwdpABchcnudfvCshTwy/V6m117SaRC5OgMiwR5qeRnYrY5RsVuq6BLZvOqbKvli3qtMf1aDSPTvJadDL07vHln5MT+TT+l9CiJn+RDAuk/Ek4lpMoXuAfh1h8nHWjRcgMhkEtFCAKIAkhCRxRj9eWtpWrDl5ZWqaS2kbSWiRy4LuyVwtZZXa0lcjfyzDQBbAHaIsDUibYX+vd1CyOMk5Ry9YgBukqvwyCfiyQm+mAA+6fBDlYgoYYja0QT9OkKClaTriBEBhyHykQOk8Y4DVSJhDiLdsEHELBMxCwCKEDWo2xBldyUi8XGatYWWw/LbdH33fCL2J26Qhvm0Q+amp8FazcokgEkAbwOYIZIFX2JW7jVLjwtBiIqYAbo++yXmbY0I+iWA2wCeEklbzdtGCzk7rTE/afEjGwDu+ETsD1wnwf+0zeTz/DudtFwWopokQ9ptmPzPBAl6gq5DfkmQpVtgRH51z5+3Bn0ciLzfOQDfIc24jt2k/RpE9cwKac9Gi5/ZSVJ+2kLIOoC7PhF7E5fJ7PukzYIrk+DGiHjn6X2YtEuCCJgi07Mf73FrEEmlAyQGkdKxyTTcJEJuQ9TTLmG3vG+ezMlmCyk7Tchv08/82idib2CMTvBPSGjaJZRmS0AlTv7cVdK4Z0krtmo7CSez8Vqh72rSvfa0ZhEiB3gXouooB9HW5QWCyh0m5Sd0bTchKp8WfSJ2BzqA9ykA8+s2kK/V74uT5nubfM1hImWMBNJr3j0teNkhE4CI/p4H8AMi3xL5cLdJU+Y77E/a9OwHiZCf0c/ziXhM+IBO3HakIVgL+cYpiPEuCViryan1iJ/XK5Ahor1ekGqU/MrLAH5IRPxXCvostJCyExpylV4z9Kw+9YnYWZyj0+/jNhEwROS7Qp89TgS8QMIltQRqfAK+3pT3IsMJupfT9P6YiPgEwFf060qHCPklvX+HiPmkZ0+xqampfn3gXoL3wRFNrACReQIix/h9AD8G8F06UcfJ5FL3ENHHmwnpmfgeMVPkv18g/zKC3aIFtYMm6yL9nLeI+L5GbANmyAT69REExCNgjPy9SxDlVNMQeb8REhLFJ13bgzxegUOK7v018uvn6FBdgoh+1ttMyhV6vQdRyPClT8SjacEvIULmh9WAGgnEMETU830i3yhpxvApC7x0Q1Oq2A12jdNB+AKiWOAziKjrEkQk1kJ7q3g+J3P5Jjpb2ngiTdOL5F98AlHRcVgShskEvU4P4j8A+B/JD0yRlvRnvR4fpBaTdZK0Y5wCPhppQy/K2k5ztUYm6vskE5s+Ed+MDyESx4f1Bb0TOEyE/h8A/BcAf0a/97VfD8UsyIe8Tm6CDlGIXkJncpA5Iv01dDnv2MumaYYexm+O+GAjFBz4YwAf0Wcm6QH46E2YEJHrLERq6jcAfgnRr1gkUrYLXnPyR+SrrvlE3MV1ej8sCRmZPJfpQb5Lvx7zgzB9oxlDpBFNiGqmCxB5yE8hytjqbdaQvyG5G0IX6lZ7kYgfQuR7Vg7pc4RIm16AqD/8gLRglPwO3xTtD3guRQwiSp6FCKgNQQTVZkl7VdC+YM5d+jkfAvjtaSWiCZHn+fUhH5pXDXMWohTtO6QFh+izfQL2r3b0gjdBiOqmcxBFHLch6lvbWaXjpTluQgwLKx/Ll+yRYM0kvQ5TISPRAxqGyBH9GYA/JQd8iE5TPxLa/2jN/XoFGAnShlUyVdsZ0FmgA10jop94It4gzXz7kP8/ChH9/B4R8HtkloZ8LXii/ccsETJG1pBF2qvexp/1gszh4UO6Sn1DxG9DpCYOOrKCEXnTZM7+mF7vkGPva8DT4UN6W6uyRM4mdufstEszrhHRL2N39OOJIuJNiKLf1UOeiGMQ9aD/M2nBSfhVMafRXNUoNjAMEaQDkdFqo6nqzfF5Dx2qVe0WEb8LEZU6SJWMpwU9U/QvAPx70oIj5Cf6mvB0klEnuRggc3WA/LoK2leVUyeNeBNiAkFfEzEE4FsQkdGD3hyNbvBVAD8C8L9CJH1jvhb0gd0pCt5mLhu7s129OTpHhbft6yZEWVyzH4mYgohkHjRJz4iEg6T9/ieI+tBR+Il5H38oK1411QRZSTWIErka2pdvXIDIT1eI6H1DxCGINqPfHuLGSuSMfx/Af6L3tE9CH2+QGa/JO0MkXEd7c4ILEIFCm4je80T01nAdJkdoQPQf/hmAn0CkOgZ8U9THPn3HIHanqJvYnWzeLpNykaw8DlED27NEHIWIbn5yiJuYoC/5Y4igzBX6M39fh4+DaEev4moAoiCgRiZluypxFklZ4Chk7KRQD5Fp8PEhbp7nT34fomtiikwNn4Q+DgqVDvAr2N0XEoAY2b/RJjJ+AlFS6UDMeO0ZInqNngf1Cb3I1x9BTP76LpHQ9OXJxxHQmvbyhiYziG59b97qUfExRLG4hUM0GnfCNA2RNjsMCSPYrZT5dxBdEyFfjny0CRpEuitO8laGGLvSrlzjIkR96oFTG53QiG/j4B0UHgkvQSTqv0dmrebLjo82I0gHfAAiGFiHmP7Qrobj3+AQ83DarRG/e0QS/meIKdEjREI/PeGjE5DI3UnTu7dAp12acYG4MN8NIt4kc/QgX4RB1Ie+BeA/EgmHfBL66DC8xL9BZAxjd/+j1aafsQgxfmPhOIn4bYgC7oPUjkoQIeW3ySf0NaGPbpExQb9vrcI5qmb0cotvYR9dG+0g4g2IqoWDdlEkIGaE/HuIwMykT0IfXSKjt8mZQQRwCjj82M5W1Onzz+IN/YxHDdZMQpT4zB/wywfJJ/w+2dLn4AdmfHSPjBrJYJNedYgxGe3QjPNE8kmIAcptJ6IJUcf3uwN+aQkin/MD7Cbr/dGGPrqNIMmiQ2SsAvgC7Rn7fx+i62gNr6h3PQoRDzPoSSXy/jFEisJP1vvoJZgkkxb5d+tEnnYEcH6H16Q1DusjfoiDtTO1tjJ9H6KA+wpEtYPfzOujl6BABHB0Mk030L4WqgXizmI7iHidPqh8wC83ANFP+J8gAjwJ0pA+fPQSvPEbUYgKnFWIbv92kbEJsWtl9SimqTcT5CATrVoT9n8KMXU76WtCHz0MFSK/+C7JegkigLODo1ffrJBlmEHLeP+DkmEaBxtHzkjNT5FJ+gM6ZXwS+ugHzRgnmf0+ybCB9qTX7hKXcBgiHtQv9LRhFqKx93+BmLTltzL56Cd/cZhk989IltvVlP4b4tSBiHgRh5tclYKY7fEOROmarwl99KNmHCIZ/oBkul2YJ27tmxhRHGy4qgTRbnINoq/wMvyqGR/9CS/if5lk+RrJdjuUygvi1r7MxIO2dHgr0aYg8oXenBmfhD76mYwDJMsvIKKoXm31UZP9nwG4+SYizkDsrD8IdFLl75MNPAQ/TeGj/6GSLH8IYAmiWyOH9uza+PJNRDToBx7EJI1D5Bo/hIgMGf4z9HFCYJBMfwiRhqhApCCOml/cVtpokgJirMVZiELud8n+9Ucf+jgpkEmm34Uof1uGKGw58lzTVxHxHIC5Q9jRGYj+wvcgRin6fqGPk0jGUZLxOewWch/JV3xV5GcQB98HF4DYS/gdiFmmPgl9nFQwkvHvkMwHjvqBLyPiBzj4LFIZIrz7bXqP+c/KxwlHbI/My+0koo6D7wdgELWkH9DLj5L6OA3woqie3EeOYgXu9RHfx8EDNAqp53chIkp+f6FnqwcCiMfjMAwDkiRBURTXMAxb0zTXtm310aNHqFQqkuM4PWPGy7KMdDqNZDLZUFW1btu222w2Ddd1dcdxnEql0iyXy0q5XPZLFYWsT5Ps36ZX86hEHMPBV2h7W3f+mNTzqY2SMsa4oijMNE1IkjA00uk0pqenkUqloKoqgsGglclkCpFIpFksFs2f/vSn7uPHj8OVSkV1XbcnyKjrOm7cuIFr164V0un0omVZ1bW1tclqtTpi23Z9a2tr++nTp6FcLpdwXZdblmVbliU5jiNxzsE5P02xAS+Kepk48BCiodg9ChHPHFAbShBDdy5AjI0bwymcO8MYcxVFcQzD4GfOnNH+5E/+BIZhgDEGTdNgmiYCgYCnEZVQKGToum4nk8nKn/7pn8799Kc/PbewsJC1LEvtge+CUCjEL168iMnJyWAikUgDaA4ODpqWZYFzrjSbzfD777+vVqtV7jhOdXt7+/adO3fOzM/PJwuFgtxoNBTXdU+TttRI9j8C8AsiY+mgZPRu2GUcbGOTV383AjGFbZps5FOjDaPRaOXMmTPSwMAAj0ajtmmabjqd1q5duwZd18EY+8aLBF1mjJmMMTcQCLhXr14t3Lt3b7tQKJhbW1uRbgowYwyBQIAPDAy4Q0NDME0zJMuyAYBHo1GZcw4AGudcJc0HAHBdl2cyGb6yslIvFou8WCwqGxsb9SdPnhTz+fyE4zgn/XD2+m2niQsliCFRB9o2pbREgOwDasMIRD3pDyEafU/kKcgYg6qqME0Tpmm6oVDITSaTUjqdboyOjrqZTEaLRCJOMBjkuq6j1TR9xQHGxMeySCAQeH9mZqa+vLxcq9VqoW76XZqmYWRkBG+//TYSiQTXNE1ijMnePdhz/b93gwFcmZ6eNiYmJhr1et0ql8t8Z2encOnSpdWFhYWR5eVlrVQqwbIst1Qq2fV6Xeacn7QDWyEO/BC7W6a2cIAmYgWiHO2g+wsDEH1a1yDaOII4YXlDxhgikQhSqRRCoRCSySQGBgaceDxeHx0dDUSjUZ3MTkXXda4oCm8R2P1aFNlsNps7c+aMtrKywsrl8n6vraFpWj0UCrFQKBTQNE3zfrZlWc1KpVIvl8u2ZVk653w/JYZuMBhsTk5Ouu+++64eDoeZLMv7+TIygJRhGDAMQ3IcR4vH48hms3xiYkK9cOGC+vjxY2xtbaFWqzkbGxu1YrGoF4tFns/nuWVZMuf8JLTGeSNCLxIn5iHK3yoHIeJBk5Fei9NViJq76ImxMWSZK4oCWZahaRouXbrEZmZmEAqFYJomotEowuEwkskkVFU12AGY9wpCsVQqFRsZGdFSqZScz+c5AM4YgyRJzPv8vT9G07SNwcHBJ2fOnFFSqdRZwzCy3r+t1Wr5zc3NhefPn+dzudyIbduXXvazOefcdV3uui4ANKLR6ObY2Fh1dHR0SpKkw3yvgCzLkGUZAEKmaabj8TjC4TBKpRKazSYvl8u8Uqk4uVyu8eWXXzZXV1dDzWYz4DgOazabfS8+xIUPATyHKIHb95wbBcCnBw2skTb0uitOCpxYLOak02knFovxSCSizszMqNevX0cwGIQsy5AkSUWbc6SmaQaz2Sybnp5muq7brus2dF1v6roeUBRFI7/yG/8nFottXL169d7k5KSuKEqCMTboWSSc84JlWY+Wl5df3L17193c3Lz4MmvFcRzHsqxavV7nruvWBwcHV86dO7fJGDvfLutGlmWMjIx4xFdd1zVc13VXVlaK8Xi88vTpUzefzyvFYlF9/vz5SZGjDyFK3x5CNEzU9kvEgyIJMQhqEicncc8VRalfvXq18t5775UuXLhgR6PRJGMsJUkSjqj43vgMpqenMTg46G5vb5cVRVkYGhqyGGODjLEkWSzyHk16TZKkK3RdEr5ZmHFe1/UzU1NT/Pz58/Ir0iIugCLn/FmhUGjm8/mMYRgjQ0NDadahL8sYY7Isa7IsY3R0dHBwcLBcKpUKS0tL23Nzc5nFxUW4rvv7IFAfQyVuXIIoCs91gogMYgziFETha19DkiTXNM2dy5cv3/rRj340k8lkouFwOBQMBrmiKMcW7ZNlGZFIhAWDQVOSpDOapjUg8rM6Xl6GKLVoLfaSZ6QyxjhjjL0icOStJTsXj8e5aZq6JEmyJEnucfj6jDGmqqoRiUSUQCAgZbNZ/NEf/RH+8R//Effv38fm5iZs2+5n0RoljtyB6F184+ly0LmmJjmj/w6iaTjQr3dK07Tm8PDw5re+9a35H/3oR+rExMRgNBo1dF3XJElSKWJ4bAEoxhhTFEWSZVljjAXpZJVecw3sDdf3pmuXAOiMMV1RFFWWZYUxphzXdxZnhKSoqioHAgEWiUQQiUQQCATgOA6q1Sr62G9UINIYz0krWvv5DwfRhuMQ4wImIZL5/RqUQTKZ5DMzM/aHH36I8fHxs6qqGpIkHSv59gjmfsjV7kgf67DZ/cbDx3sesizj7NmzAFC2bZuXSqVApVLpV9cnTBy5QWR88CatqBzgoekQY/Lfg0jk920uKBAINIeHh2vT09Pq6OjoGV3XY/R9/NatLsIwDExMTOyUy+Xm8vJydGNjI1yv1/sxPy0TR96DKBt9Y4J/vzkcbwTGOWJ6pF8fNmOMRyKRwuTk5Ny5c+eWAoFAig4kn4Q9ANM0jfHxcX7p0qX88PBwuY+/ireK7Rz2MVR7v0RUySwdpx/Qt1U0mqbVJyYmCleuXCkNDw/b3TTNfLwU8Xg8Hrlw4YJ7/vz5hqZpbp8+I4W44vFGPSoRPbN0BmJ5RqhftQdjDMPDw9XLly/Lg4ODFxhjV3257z3oum5kMpnkpUuXgpcuXXIVRenHnAYjrpwn7uiv481+NJtMqvVdiE4Lo09JyDVNa1y4cEGenJxMmKYZpJvjo/eelR4Oh5UrV64w27al1dVVbGxswHGcfvsqBnZ7df8JouTNPqxGNInV59HHxd2yLLvj4+PPLl68+GJwcJAHAgF/ikDvQlIURY3FYsr4+Di7ePEij0b7spLSKwb3+GMe1jRl9EFvQ6yp6ssCXUmS3Egk0vjggw8Kly9fLsXjcUdRFN857P3DkycSCevKlSuVVCrl9OuhQtx5m7jEDkNEGbujxlP96hsGg0F3fHzcunDhwqVoNHpdluU4/ChpXxyg8Xi8cvHixY3h4eGiYRj9mOFnxB1v9YR8UCIyiEhPHKLI2+xX4U2lUtKVK1eMWCwWliQpCH/ocd9wUZbl8MDAQOCjjz56cPny5SVZlnkfEtEkDsWJU+ygRIxBJCaT6ONtTqlUSpqenlbj8bhMbTo++keIVU3TIuPj44nJyUkEg8EqY4z32XfQiEMjxKkDEVEGMAHRd9iuNVTHbdrg0qVLuHHjBoaHh1kwGISfN+xDNjIWDAaDI8PDw9Hx8XFomtZvWrG1h3fiZRaZ9BoW6xCRnuv9aJZKkoRwOIzLly/zK1eu8Egk8roRFj56m4iypmmhbDZrTk9P68FgsN9OU888vU6c+oOcovQaBkeIvWfRh/k2RVGQyWSc4eFhZ2BgAKrqZyv6GYqiSOl0Wj1//rySyWRYHz5Pnbg0QdyS9kNEFWJf+DD6dDqbpml8eHi4kUwmLU3TuG+S9jckSUI0GsXY2BiuXLmCRCLRb1/Bm/Y2TNxS90NEnZg7hD6NMOq6zsfGxhqpVMrqw0ibj5ebqEgmk3jnnXcwMjLSj66GTJya2GtlvuqbKBAr1hLo0yBNLBaTJicnIwMDA2E/VHpyiBgMBpFOp5HJZHg4HOZ99mgl4lQGeyrUXkUyjVRoX1bTjI2N4fvf/z6y2aysqqrsm6Uny0QNBoPWxYsX1yYnJ0u6rrt9RsQ0cUt7ExEliBmNwxAVAX1DRG/M/eDgIJ+amnKi0ajrk/DkQdd1NjU1JZ87d84JhUJNHH119nESMUXcCrZy62UkUyGSj2mIkKvURw8I4+PjfHp62k2lUq43+t7HyYKiKEoqlYqNjY0pg4ODdiAQ4H1ERJO4lURLwGYvyRh2520k0GedFqFQCJcvX8aNGzdgmqZ0yEG5Pnrf8mGqqqqZTEaemJhAPB7vp2CcQtzy5j6xVxExBVEpPtCHJgsymQyy2aykaZo/g+aEIxqNytlsVolEIv0WxxggjqVeRUQvqjND732DcDiM4eFhJBIJpus6803SE68VEQ6HlWQyqZim2W/Pu5Vj0l4iMlKbYWJsX80sTSQS7tmzZ61MJmP5JDwdCAaDciqVwvDwsJ1MJvupXzFAHAsT55i0RxsGILL/fdcqZBiGnUqlSrFYrIwD7KXz0b+gNePNy5cvVy9cuNCQZblfoqcycSxCnJP2EjEEUSUeQB9FSyVJ4qFQiCcSCRYOh32z9BQhHA67Y2NjzYmJibppmlYfETFAXAvtJaIMEVpN4tU7F3oSpmk2M5mMk0gkTE3T/O7706UVVcMwtEQi4SaTyUa/uLjEsSRxTlb2/GWQ/kLtJ2GempranpmZ2U6n03GIglofpwcqY0zWdZ1Ho1HWR0RUiWvBvT6i3GKa9o02lGW5Njo6Kg8ODiYDgUDYl8vTh1AoVJmYmFiYnp5e6KPLllpM029oRG/j6QD6KJE/NDQkDQwMhEzTlBRFUXyxPIUqUVWVWCwWzGaz/eSSKMS1KABZ2vMXSYg6uL4R6GQyWU4kEtVAIMBpm5OPUwbGmKYoSiIWi8XPnTuHPjmPFeJaEoAivYShI+ijTcDZbDafTCa3dV1vwg/SnFbIsizr0WhUu3LliqPrej80gqvEtYG9RFTJZu0r03RiYiKYSqUMVVV9bXi6mcgjkUhzYmKiTq1RvZ5L9hRfDICqtDiOCkQEp692WyQSieFgMAi/9/d0Q5Ik1zCMRiaTqQQCAUWSJNVxnF5Wi4y4FgSgtBJR6yeTVJIkHo/HLWr87akCb9d1ueM4TcdxbNu2Fdd1Vc45I3+Gy7LcVBSlCSCgqiqXxMwH1gvfwXVd2LbNOecNx3Fg27bKOZfpntuyLFuyLHNZlhltafpGX1035UHXdScWizWDwSCXZRmu64Lzni+yUgFoSoua7JvtSIwxBAIB57vf/e6LdDodU1U12guHCOcctm2jWCw2t7a2crlcbmd5eTlVLpfTjuNonHMmy3IzFostj4yMLEUikZnR0VE7HA7riqIE6EDp0qVz13EcXqvVlNXVVadQKDxfWVlx19bWRhuNRgQANE0rxuPxp+l0ujk0NKSOjY3ZiqJckiTJZIxJXT5IZEmSQpqmaYZhuKqq8mazLyb06wCCrUQ00EeF3rquS6Ojo5FgMBggIeg6CavVKu7du4fV1VVpeXk5PD8/L6+srIRqtZrkrRSTZVkyTdPMZrPpoaEhdWhoSL527ZoyMjKCYDDoBRnYMV97rdlsrqytrZXu379/bWVlRVpdXY3mcjm+tbWlWpblXbsWCoVSg4ODzsjIiDQ+Pl4+e/bsv549e3bSMIxBWmfQLTIyxpgUCoXKb7311i+2t7e/3Wg0hmzb7vV4RwCAobSoRy/L3w8akSuK4pImUbpcXNpwXbdULpfdhYWF9M9//nMsLCwo9Xo90Wg04o7jSK7r/t7sdF1X2dnZSZTL5djCwoKsaRoajQYsy6qPjIxUTdPUFEU5tsIEy7JQLBb5ysqKffv27cbHH3/M6/W6ZFlWptlswnVdyTPvXNcNFYvFQKVSweLiIr9z504lmUwu//mf/3nh3Llz4UgkomqapnbrcTDGYBiG+4Mf/KDy5MkTO5/Pc9u2e12cgwBMeWpqCtjtyn8HwHQ/RMii0Wjzo48+KqRSKVlVVa1bWtFxnEqpVFqZnZ0t/u53vxv86quvkM/nYVmWREL8B74f55y5ris1m01Wr9fZ9vY2SqVSUZblzUgk4gQCgchxHC6u6zr5fN6dnZ1VPvnkk/Bnn32W2tra0huNBmzbljjne81NxjmX6NrlWq2mFgqF9M7ODg+FQlY4HOaBQECRZVnp3hnNdEVRxubn581cLqfUarVum8xvwjMAX7dqxFA/aETGGBRFYaZpKpFIJKGqqtItEnLOUS6Xg3Nzc0O/+tWv+MOHD1EqlQ4cIFhZWWGVSsW0LEvRNI1duXLFMQxD7jQZK5VK9cWLF8179+4F7969G9ze3j7wmei6bmx2dlZnjM1qmrZgGMaYqqpnuyT8DIDGGBtIp9NfR6PRcKFQGLRtu5ddriCAkNTiI4b6wUdkjNmapuWGhob+r3A4XJEkSenWiec4zmo+n5979OhReXZ2NrG1tYXDmkLFYlF/8uRJ6KuvvnJzudym67plAB1tdt3a2sKDBw/w4MEDHIKEu7Z5oxH86quvUrdv3049f/48WCqVui0jLJlMmkNDQwHDMHq9bjqwl4h9EaxRFMUdGhra+fGPf/yVYRgVSZK61QxaKxaL23Nzc/l79+7VC4VCO4jB79+/7zx8+LBYr9dznPN6h0xSFAoFzM/Pa3Nzc8G1tTW1DYfS4Pz8/PkHDx4MrK+vd90UDIVCZiwWC+q63g9ENFqJGMCeoae9CF3XpeHh4fjExMQNRVG6Nu7RsqzaysqKMTs7O764uJhpR1Cg2WzKa2trxtzcnJbL5QqWZTXQ/goR7kwdxOsAACAASURBVLquvbi42Hj48CGWl5dVy7KOnDbhnKurq6uBx48fy6urqxXO+XqnNfrrEIlEdkKhUEVRlF7v2tcABDwhliDyGT2f0FcURY5EIglFUd6DGDXQldO3WCwqS0tLsdXV1cFarRZpk88pNRoN/enTp/Fbt26lS6WS7Lpuu4XZdRynury8XHj69Gl9Z2cH7bqHlUoFCwsL9WfPnj0tlUq3XdftSiKPMYZ4PN6MxWKOruu9ntFXAeitRFTRBzWmkiRB0zQdogG4a1uMd3Z25NXVVWVnZ6etP9+2bWl5eTl67969ifX1dcmyLJu3sTyEc86bzaazs7Njb29vu16OsF0oFArunTt3Gvfu3Svbtt21jLphGLFIJBLSdb3Xax8VAKrUEm2S0OMNwZIkcVVVm4qiNLtdXW9ZVmlnZ2enXC53ZE5KqVTii4uL9VKp1GwnER3HkRqNRrBSqQxalhVzXbeth2+z2TTX1tbeefDgwV9YlmV2K14DIKuqaqIPelQlAFJfNdIGAgFreHh4c2RkpAaR9+waG7e3t1PLy8vY2dnpyOHFOZfr9XrCsixG+bx2odxsNp9XKpUh27YT6MC0Ps45yKS2yGrxK/L3wUZQQMBFjy/zCAQCSjKZjJmmOdBtjWjbtmxZluw4TqesCMY5l9tMQpRKJfWTTz6Jz83NBSzL6si1u67rNpvNGud8DUAN/njL194uAK7U8psmgJ6uB1IURdI0LSjLsonuV0v0RLfEQdFoNJTHjx9HVlZWtE6Vf3HOueu6Due81k2Zisfja6Ojo4s067ZXYQNothKxQWTsZR/RDQQCNcMw/CHCh4QsyzUAL2zbLvMO9QgxxhiNLQmiiwHAdDpdmZqa2slkMr0877QJoCG1sLJONn3PQlVVOxaLbaVSqaVum6aMMYcxZjPGOnkg8DZ/PjcMIz89Pf1JLBZbkSSpI3k+SZIYtXWlINJiXXlYwWAwnk6nM/F4vJdLNy0A9VYiVomMvewjslgspkaj0a73Teq6XtQ0bUeSpEYHSdj2kQ+6rss3btwwJicnFU3rTP0GY4zJsqwwxrwZud06LOOKoqR1Xe9lItYBVFuJWOl1ImqapmqalgYw3m3/LBQKVePxeMUwjI74QIqiuJFIpBwIBBpt1IqMMZYKhULfz2azQ4FAQOqAZeEyxqqKohToIPHxZiJWpBY7tQIR4eplMOq06Ho43DRNPZvNBuLxeCeuhRuG0RwdHS1Fo1FbkiTexhuoaZqWiEajqmmarizL7da4djab3R4dHX2hKIrt8+yNqLUS0e4TIgI9Eq1MJpPl4eHhYjQabbb5pEEgEHDi8bgdCoXCiqK0tXqICSbqyWRSTafTUiAQaOu1DwwMSO+++27oxo0bCVVVu10gwhljbidM/E4RsQmg3CdE7AmYphkYGBgIRqNRuZ3mna7rmJ6exvvvvy+bpmm2O9jBGGOKorBMJoPR0VGEw+0bBhAMBnkmk2FjY2NmKpUa6PbAZ1mWLU3TSpqm5WVZrvcwEct9FazpMRs5HYlEshMTE8b4+Dgk6eiHv6ZpyGQyuHr1qnLt2rWAYRgdEWRJkjA4OIgzZ84UBwYGthVFacsBnEgkmuPj45VkMllHDywyUhSlaRhGORwOFw3D6NVNUXUAVaWFiDWIXKKPfcrzwMAArl+/zpvNprO1tcVLpZJyBGK78XjcnZ6els6ePSuFw2HWqVmtkiSxWCwmT09P55aXlwtra2sT6+vro0dJK+q6jtHR0drly5fXxsbGvM3T3YYmSVI0FAo5kUgk0O2G5VegAaDWmtC30OMJ/V6DruvIZrPVmZmZhRs3bjw/ymcFAoHC5OTks3feeWfZ29/QyVypLMssmUwOzczMTF66dCl+1NromZkZvPPOO4GxsbEBXdeTPfKIFMZYUFXVkKZpvdri1wRgKS1E9LRiFbSzrceEfjuZTL6Ix+MhAOd6QiVKEgzDkMbHx9WbN2/WbNvO3b9/P1Mul9WDaBfTNMsXL16svPXWW+zMmTNaJBJpi6m7D1M4PjIywj/44ANJ13XcunUL29vbB565c+XKFdy4cQPnzp3TIpFILwm85D2n47ifBw0kEd9qAL4x87EJoABgA8AQeqxJOBqNFs+cOfNsYGAg2StEJM2imaaZOHfunCxJUnlwcNB9/PhxPZfLsZ2dHfV1ReGMMVy+fLl45syZ0vnz56UzZ84kw+GweVxCI0mSFI1GMTU1BV3XEQ6Hsb6+Dm8Sneu6rzOlEY1GcePGDVy4cAHnz5/HwMAA03XdXwS0P9jEtQKApvKSv8hBLMfoKSLG43E1k8mYwWCw16okZFmWg+FwWJqamtITiYQ8NDRUfPToEVteXjYqlYq2dweDN5fVNE189NFH+bNnzyIWi4VDoVD4OE0oxhhUVUU0GgVdA/L5PHRdd3K5nFOr1ZjjODJa+lQZY1zTNCcQCPB0Oq1+73vfQyaTQTgcRiAQ6EXN06toEtc29mpEG8AWgCUAM7121ZFIJBIKhSYZYz23FoAxJimKEgyHwwHDMBAKhZTBwcHm+vp6I5/Py5Zltd5nLsuyHQgEmul0WpqamlJjsZhO83e6UiCtKAqi0ShM08TAwABXFKWxsbFRLhaLcqPRCLuuq7X8W8c0zUo0GnXC4XD8/PnzzCfgoTXiEnHuG0R0AOx4DO21qw4EAoamaUO9MF7/dZyUZRnpdDqSTCZrZ8+etXZ2dtxGo/ENLSTLMg8Gg04ymYQkSRn0QKUQzYtFOBzGtWvXnEajUS+VSkqlUgl56wKIiDwWi1mRSMTxudQW03QHgLOXiBWyWXuuRlCSJLmLS1oOY68GTdMMhkKhP5B5iK51rUeXaTIAYV3XTU3TkEwm916kSl0V3r/1cTi4xLXKXiJ6UZwy2a/cv9Ht0TT9e+nsdWT1cXhwfLOa7RtjGBz6iy2IJKNfOe/DR+e0YYO4VgbwjdC622Ka1n0i+vDRUSLWW0xTV3rJXxZJXfrOuA8fnYFDHCt6Sk/aY7faAEoQ0Ry/ANyHj86gThwrEef+YFSfC2AbwJf03jverai7sn2T2ccJQCvHXOAPJ3tzAJsAbhNjewaWZdWazeYG5zzvP0cffY4N4tgmce6lRCwBeEps7ZnEfqFQKJTL5Qec83n/OfroY9jErafEtZcSERD5jS0A6xCh1Z4wBbe3tyubm5tLzWZz03+WPvoULnFqnTj2+7ZD6RX/uAZRB7fZK0QsFovBhYWFdD6fj/nP00cfE3GTuFVr5dar6jYt+sfrvULEer0+vLq6+ierq6tv+8/TxwHAOzXR/JBEXCdufWOYt/IaO3YNLVGdHoC3lMUXLR/7JiEAl3Zx9MLaQS8rsYY98ZdXEbEBYB7AMvzEfuvRCtu2y+Vy+WePHz8elyRpMJPJuLFYzFBVNdXO0YT9dmts24ZlWdVqtbqzsrIib21tZUZHRzE0NIRAINCtmtuG67rlarVql8tlE4DZ5fvkEKfmsWc+1KuI2ASwQiq0CCAAf8cdms0m8vk8+/zzz/XPPvtMNU1TnpiYQDablSKRCKLRKBhjSKfTCIVCJ75Hz3EcvrOz0yiXy3KpVJKLxSLb2tqSnj9/Lq2vr+Pq1avNYDA4Pzg4GGSMZXDMzeaO43DLslAul1Eud30hlENcWiJuNfdDRJf+0zyAZwCiAIwe8BPdQqHQLBQKjq7rhq7rxybsnHNwztFoNNRGo3HWcZzo4uJiZHV1lYXDYSWRSCCRSECWZVy9ehUDAwPQdR2BQIDrut5QVZVDzCjtS3Zyzm3HcXiz2ZSbzSazLItXq1UsLCw0V1dX2dbWlry9va3t7OxE6/U6NwzDLpfLTqPR2IKY6DZwnNdr23Z1Z2fHXVlZCWxsbKDRaGhdvoUN4tI8cesbLp88NTX1Ovs6QDfwPBGx2+0vtizLZcZYmTEWCgQCFVmWuSTY2NFr80wrSZIc0zS3dV3XGWOhSqWira2tsRcvXvC1tTUpl8uhVqtha2sLhUIBlmW5jLESgJrrujJjrC5JUhOiJ7HX/SvLdd1ao9FArVZzisViY319XV5eXsazZ8/4V199Jd2/fx+PHj2Snzx5Iq2trTFJktjo6Kg7MzPjnD9/XhkaGkIwGAzTQppOH0LcdV1eq9WQz+c35+bmcO/eveDc3JxeLBaVLsovhyjw/gWAXwN4sdflU96gSucB3AfwAwDJbpun+Xye/e53v5OWl5fl69ev29euXXs4Pj4+YBhGWpblADo8jt8bKRGNRhXTNOXx8XF7eXm5MTs769y9e1fb2toKO47jLi8vs1AoxIaHh3H27FkMDg5K0WhUikajO8PDw0/T6bTCGHufxsG3XjMnwh+nwHDO+d6f53DObdd1c5VKZXVhYWFsa2troFQqSfl8Hpubm1haWmLPnj1jAHRJkiDLMh8dHbWuXbtWvHTpEh8cHIzGYjHFMIzx4/APuUCzUqk0Z2dnA7lcTvrqq6+k2dlZuVgsdtut8pqA7xOn/iDuouyDxTmI5OMoachuakWl0WjEHj16xB8/ftz4+OOP5//yL//SOnPmDAuHw6aiKCHGWKcjJhqAqaGhIQwNDVXPnz9fyWazDcMwjJWVleDq6upmPp8PV6vV4NzcnDQ3NycDiAPgwWBw/q233nr8wx/+MJBKpd7Rdb0uy7LOGJMVRWmqqmqpqirLsnxcA7Katm279Xpdtu3fj01xOefFer2OUqm09uDBgyc/+9nPwrlcbvQPboSmIZvNIhqNutFo1D137lz9rbfeKo+Pj6skK8cj5a6LRqPhVqvVei6Xq/zVX/2Vtrq6mmk2m6wHouwcIlWxRVwq4CV7ONhPfvKT1wo+gLcB/G8AbpJW7BUfhyuKUr5w4cLqu+++u3bt2jU2ODg4puv6MG04ko7h0OCu6zqWZfF8Ps+WlpYquVzuX7788suZXC43ms/n1Xq9/nthYIw1A4FANR6PI51OB4aHh9cHBgaSmqYFgsFgPp1Ob2Sz2ZBpmqN7zGJHluWmJEkuzexRyDph+xQEh3PedBxHchzn90ttOOf5fD7fePHiRWhrayvEOZdc17U3NjYeLi0tJbe2tsL5fN4tlUoB27Z1Ms25pmluOBzm2WxWuXbtGkZHR+vZbNZJJBLMMAymiGnFHQ/MkN/erFQq9tLSEmZnZ2u3b98uzc7OjliW1SupLpdI+GsA/ztEjal9UCIyAGcB/AWA/wxgqteip4FAoJLJZCpTU1P80qVLxtTUFB8ZGSlLkpSk4MixBHJs20a9Xrc45yulUik+Ozurffzxx/zBgwdatVqV9/qbiqI4kUikHovFdNKCjVAoVE8mk6ppmt8YdGMYxurg4ODDTCZTCoVCcUVRRiVJSjPGjH1cW9N13bVarfbg2bNnkeXl5bccx1EpqmiVSiUnn88rlUpFAcBc13W3trZ2SqWSbllWgPNvduiYpmlduHBh691337WnpqZGTNNkwWDQ0XWdq6rKjsNf9+55s9nkpVJp9vHjx8t37twZfvLkydnFxcWmZVnGS8ztbkZL5wD8DYD/SgEbfhDT1DtNt4jFP4QY7NtTRGw0GkYulwtWKhUUCgV3c3OzdOHCheLw8HApFotlVFWNHkcgR1VV0Aq1UdM0meM4z+fn57eePn06Ua1WB14iRHI+nzcKhYI3G0aXJMnzt77x+bqua8lkMp7NZrVkMhk2TVMPBALyfnZjuK7LGo2Gvrq6mpibmzM2NzeZNziYc666rqu6rstahglLruvGSNuwlxx8yGQy0sTEhOQt3/GGeh1XrrDRaGB5eRlbW1tYWloy7t69G3v48GHIsizNcRy1h0joacR14tAWXrEebj9zNMsAHtNrGkAKXZq/+YqTkTmOw/L5PGq1GnK5nDY3Nxe8fv369vT0dCOdTlfD4XBTluVQp80lEkTJtm1wzmuc8x3Xda3XkWSP9fFSVKvVaKlUurC0tORqmiapqqooiiLvJ3XDOZdt247XajWzVqtJtm3L+/iZ7DXXLNm2HWg2m85xjrInq4PX6/XG1tZW/fPPPzefPXsmr62tZdfX19PV6u8XKvUSCb1ZwR5/yq/zAfejWvMA/pU04g0AkV6LtTuOg0qlIlWr1UChUMhsbGzE5+fntcuXL29funQpHw6HxzRNUzu93IVzjnK5jIWFhbH5+flMpVIx2/CZMuXvUK1WD3w+0HNuy+FZrVaxvLzsbG5u1sgHi3baSnIcB/V6HYVCAcvLy8Uvv/xy/e7duxObm5tmo9HQbLtnFxNXAcwSd/J4TZXa6/KIex+mBLG7/gxEqVCvjtSTbNtWisWivrm5KW9tbbGVlRVtdXXVDIVCslfx0ikycs6xsrKChw8f6g8fPjR2dnYUnCC4rgvLspqapj2emZn5B03TOppjdl0XxWIRt2/fxr/927/h3r17uH37trq+vm42Gg35dfs5eiBamgfwGYCfQVTU2EfRiIAox1mgV5HMU7XXBWZnZ4eVy2Vzbm7ONE2TWZaF6enp34+XN00ThtH+gqHV1VUsLi6iR/fxHfW+SsViMXDnzp1wKpUK/uAHP5DC4XBb18g5jtNoNBpWqVRSdnZ29FwuJ/3yl7/E3Nwcs207ZNt2qMf8wFeZpcUW3rx25eF+NaJHxjSAMYhqG6NPBIc5jsMsy8L29jY2NjaQz+dRrVZtSZKKhmFsuq4bliSJHVGQXNu2nfX1denOnTu4f/8+8vk8TmK3COdcsm070mw2J8bGxgzTNGVN09gRbiAH4JLp7W5tbdVevHhRn52dlb744gvt1q1bbGFhAdVq1fOr+2HAcQFiLs2/APgCb5h2oRzgRjUAfAXgcwDDAGLoo0Jwx3Hw4sULbG9vI5fLIZ1ON5eXl1crlcqLeDyejUajcjAYhKqqhzFdueu6Trlcbn766afS/fv3pc3NTbTujDhhkGzbNjY3N/XHjx/vpFKpmmEYAUmSggfxRb0ATLPZdBzHaRaLRWV5edldXFyUFxcXjbW1NW1jY4Ntbm6ih03QV8VVcsSVr4g7vF0aERBdxTr5isM4xuqJdqHZbGJnZwerq6vOixcv6mtra03LskbK5TLjnMNLHxAROfD62fNEQrderzeXlpbsv/u7v1MfP34sNZsnf/my4ziwLKs5NDQ0l0gkKqqqBt9U2URpEU6+Jt/e3nYWFhbsXC7Hnzx5oty7dw937tyRHz58qK+srCiVSoX1oVVRBHAHwD8CuIc9TcBH0YgeKgAWATwCcIm0Yr/6OoFisThx9+7dibt372JoaAjf+ta3MDU1hYGBAYTDYRiG4WiaZquqqkqS9CrtbzuOU83n882vv/7aKBaL7DQ0L3POUavVpPv370djsdhWIpHYGR0dTWqaFnnVxi76P7Asy7Zt2ykUCvKTJ0/cTz/91N3Y2NA2NjbkRqOhnID7t0ocWSTOoN1E5BAdxnMQFeQXTopgLS8v42//9m/BGEM2m8UHH3zAL126VB0cHCynUqmorusBypntJWS10Wi8ePTokf3Xf/3Xf3QaJwjcunXr5pkzZ+qGYciZTKYpy/LeiiaHc+42Gg0ll8uxlZWV6tLSUu2LL74Iz87Ohjjn+gm7JS+II9tvMkkPS0RAJCgfQIyD+y56PHp60FOec47NzU189tlnbG5uLmQYhjQ4OJh/++23d6anpwuKolyXZTnomavlcjn06NGjM7du3eKndYxHo9EI3rp1697o6OhSPB6fDgaD1+h+urZtc8dxfvP48ePlW7dufXdpaWm4VCqZpVLJyOfzJ3H0SZO48YC4gk4RsQGRE/kMovb0j0/anbQsC6urq9jc3JQlSQo8ffo0vLa21nzy5Ik2MDDgRqNRV1VVmXOOnZ0dZW5uTn769OmpHabjuq68trZ25pNPPjEajUYqlUpxzjl3HMfO5/PS6urq8KNHj8znz5+HqtUqHMeRHceR9lQWnRT8lrixhD3jMNpNxNbeqt8CeAsiwX9iRml4RdxUsSHX63WzXq/rS0tLbiqVkjKZDHRdh+u6KJfLWFpaYltbW6d6Z+D29nZybm4uatu2kkwmGeec04HG1tfXR/P5/FC9Xg+0RD9P2v3y1hr+lrhxoIW/h636qBPj7wF4COAKgBBO5gJL5rquXCqV5EqlwtfX191cLiepqgrOOSzLQrVahWVZp5mH4Jzrm5uber1eRyAQAOecUYUTazQawRNutntLfh8SJ5ZwwCVOhyWiN9NmDqJ8JwuR1lBP8t12XZfV63W5XvcXZb3CV0Sj0WjVeKfFSvCKu39GnCjigGNIpSOcABZE0vL/gwjVFuGPXvRx+uBNZ3tEXMgRN/hxENHTiiWI6vLfQORMLP+5+DhlsEj2f0NcKOEQQ7mP2kzmrfv+JYCvAez4WtHHKdOGOyT7vyQuHKoWrx1dnTZ2e64e4TXNjz58nDCUSeb/lThw6MbIdvTKcbKRPwUwBNGhYeCEB258nHo0Icbnf0qv4kH9wnYT0VPRXwMYhOjiT+GYJzv78HHMKJDMf0LvR3LJ2jlwpE7q+WNyXv21TT5OKjjJ+Mck80fOZ0ltvrg1iGlVn+MlY8V9+DgBcEi2PydZX2uH0mn3CK4KxNzGX5ED60dRfZw0Eu6QbP+KZL3Sjg9u92AjF2Jgzl2IapthiPK3sP8MfZwAVCGipL8lGc+jTYt8OzFhrAERTfqMiJiAmBbuR1F99DO8KKnXXbGMA3RXdIOInJzXOYgk5yhpxCxOT+2hj5MFDmADYvzFL0m262hjQLJTMze9Vql7EIWwKYgFNppPRh99SEILIkXxM5LpQrt/SKfnpW9CJDtvkSp3/efqo8/gkuzeIlne7MQPkQB80MEv4UDsC/97AD/FG6Yd+/DRY7BJZn9KMryCDmUBJLQhGfkGtV4lm/oXAH6ONkaafPjosCbMk8z+gmS4ig4VqshTU1OrAL4NkaTsFBmb9CVqADIQi0t0nKDxGj5OFJpkgt6G2Gn4BcREto7lxL1gTYF+3Smz0aEv9m9EQgli1k0CPbTizYcP4oCXC/9vJLOb6HBhihes+Zq0YidNVAti8OovIPYBPEabqhJ8+GgjKiSb/0KyuopDdNwfxjRtJUsQne0ndOnzaxDj+uOkITX/+fvoAXj9hf8MMS7/MY6pRLPVLFwEcJNOgE5qRhdi2pXXsygDuEiHgA8f3UINu0UoPycZdXFMXUR7/bPPAMxArJPqJBmrEJOQA0RGFWItuAo/4e/jeOEFE59AFHL/gmSzepwXsZeIDYhhwceBPESVgreNOACxe9GvvvFxnCT0hj/9AruVM/njvpCXrWXLAfgOXVynb0IVIiJVhhhQ7I3ZkH0y+jgmEi4TCf87RPVMHl1oan/VfkQNIs93HIOgbIgczTJpY5+MPo6ThD8H8DcQY/IPNQqxk0TchsjzLRyjjV6EKCEKQ+QXw/AT/j46g2aLOfo35BN2dUD26zYGLwB4D6LW7jjJuE2aMEIvv4/RRztRg0hL/IzM0fvogSn1b6pqqZJ22j6Ga/FGl98mTegSOadwfAEkHycbZYgUxa+IiLfpz7o9ziXxJiJ+CZFb/PUxXZBHxi9atKRDZAzBL4fzcfg4RAW7ecJfQERHjzSLtI2YeZ1p2mqivg8RTT1O86GA3QbMEEQAR/P9Rh+H8AfzEGMP/xliUcx9HGCtdofxPoBP9kNEQAROgnSCHAe8fXObEOuuLIgobpQI6UdTfewHLsnQHSLgP7b4hL1AwlG6js39mnoPAXyIzrVKvU4zfgFgHWJmiAvgXYgaVd9M9fEmczQP0T3x38gc7bUpERMQw6iwX40IiHDvRzielMbLCLkBUQfrQuQaTZyuZZg+9m9NeeMt/gGin/B3EIOAHfTOBPqPPBIelIiA6OafQmcLw18Gh8iYJ+1YIbXuJ/597CWht0D3/4QYb+FZVL20u/M6gHm0tAEe1Lxbg9j4lIVIvh+3071Oh0GZTr13AFyGWHjj5xtPN5pkNX0NUar299hdo91L0+azLVzCYTUiSBteR+drUV/lfFsQ0dSnpCEViIJxnX4t+TJ5quCQZlmE6B767wD+H4hx+GX03sqH6xB7M3BUInr+4s0u+YtefnGHtLLXQR0kv1HzyXiqSLgDEUz8OYD/G2JN2hJEJ1GvDSm7CbHiG+0iIiDSCm/j+COprdqxTNexTlrSxW5pnO83nnx/MAexGu0fAPwTRGpiE705JfBb5K9a7SaiRdpndK+9e8ywIJKzq/RuQQRwVPgFACfVF9yGSND/C4D/FyL6+JRM1F7cy3mV3KhX8uQoRAR9+DD5Z4UufUkOkTOqkrOeo1ORk8+oESl9c7X/zdASuUO3yA/0Gnm9IF4vknACojLswev+0VGJCPLTLpOZWO/iF/bImIfIIS3R71XyH2Uio0/I/iNgg7Tg1xBlan8PMf7+GT3vZo9eexzAJMQ+RXSaiCA/8T167+ap5OWR8nQtm3SKNoiAeot29H3I3vcBbQrGPIWINP4zBWU+J+ungd5dES9BVIH9dj//uF1EBJkMNyESlb0Am2zyRwCek7b2Jg8oLRrSR29qwSqZnF9C1In+V4jOiQX0VnL+VThQ11I7iQgiYbfSGq86VRsQgZwvAHxFJnQEooA84Mt8T6JEPtXfAvg/IIq1F4ic7kkjIdCZwunbEHV0v+khMnrd/w/pId8DcA2ikP1D+FU5vYImmXK/pWc0BxF8K6F/Fhd9RBxAt4lYodPsw/3ax8cElx7oU/Iv5slknSOHehTAIPxZOcdtgpbIYnlBz+YziHzgEh2eVh+R8EOS/UovEBEUJHkKMZbx4x4jYwOiCKBCPshDAJcgitmniZQjZL4q8IM6nQrCFEnbPSU/fo6EeAkiFVbHMU7abgO+Q9/lUItMO9nTt0ya5dsQZUe9JAhefWINu6Mc70AMOL4BEQGeJDJ60wH8/sejwUsvVYiEXiT0DkTJ5DYdkL1YmvYmfJt82OXDfkCnhetFy2nxcQ/eQJfImKOTVj7BagAABJ1JREFU+BmZq48AnAMwDuA8gAsAkthNe/h9kPs78LyXSySbhZigtgAx4v4r+nWvVsTsVxMu4Iilnsdxyr8gDdRrPuPLBKdM5tFTiGTsOMQukHeJkGkAKXyzuNwn5B/eR69Lpkym2joR8F8h0hELELneRh8T0PMJnx5FEx4nET0z1UJvRVNfJ0gNEp48+ZD/RER8m0zXYdKQMSKlv/14twKmTD7eFlkZdyCiiI9byNfsM//vZfiIDu3NdnzYcfo93sbg4xzPeFQ/0hOuCgnXM4hC4zgFdK5C9JedJX+ytYzuJJfTuXte3hjMZxCbdu+TuZ+HiFBvYbc3kJ+A73+TDpe2Ldo97gBEhUj4XdKM/eCUe1G+AkS51QJE3jFGPs4tiMLeYYjpBYk9JuxJC/LYe0xOL9i1BJESekzvBezOpeUn5LtLpAl/1e4P7paQ/IpOlfvowgqsNpDSgSif2ySfJwIxAmECQIZIOUyETECM8khAVPK0ak3Ww9+zVdvViXAb9L5OxFui+zAPUfxfJAvCaQnUnBTEyQL6VSc+vJun9a8hwr7eSdpvAQlPWG3spkGeYLf1Kkh+5CT5ljNEyCB2R3t4BegKvVQcb+7SO1ia9G5jd9VBgwjoTdD7ksyxp2Rq1sjvt0+Q3/cqeBZPx1yqbptNn1DwI0LasR/R6k/W95gxSxDpkC+wu+EqQmZtkkzXGBF0mPzOAYi85XHAI5mXvtkgk9KbfFAgLVeig8brZmmi/3J9h8VV4klHc+G94L/cIa3xLYj5kycpoNHA7gQBT/MFIIoETNKOIYgC9CSRMEZ/rpLWDNDvgy3aNIDdhufWgVlui4az6GDwtJr3qrdoMG+1gRdQ2cFuoUOZfl1v0ZQnzdx8E75FpvfTTv+gXgkkPKUv3A8R1aOYsQ4RoEKCz8hnlF9innrmrUGkDbW8jJeYt3iJWelVsnivMv1Zq1nZapY6Lf6d0+InnibyebgJEeU/jmW9PRXRKxMJPyRfa+UEPtxWM/Z1XeXSK3xHteXPlJcEfVqDLHbLq/kKX9CFj73IQlRVHatC6MXQ+m8hcnODEDmp04hWMvk4PlxvkcFjRa8mnO9C5KY+8mXDxzHhI5K5rhz+vZxsXqPXhxDpjRe+rPjoAEYh0hNdLb3shxKs31Kw4n1fZny0Ge+TbHW9GaFfyq8e0vtNiMTyti9DPo6ABESBRc9E6PutKPnXEInv93xZ8nFIvIcOV8mcZI3Yii9btOMcTmaaw0f7kYUYh9KTeep+btP5NURy+zu+jPl4A75DstKzxSL93qLzhF4fQBQEfOnLnI8WzEAEYz7u9Qs9Kb1yn0KUe92EmDez6svgqcYgxES+zyDK/XoeJ6mDvEGmh0aE9KeunT4o9Ow1koVGP134ScMivS5DdDJ84svnqcC3ITpJ+rJp4CRrja/p/TpEp8KnvqyeSHwA0WnS1wfuaTDfvNrBG+RH+oQ8OQRsnJTneZr8qDv0fg0ikuabrP1rgpZP2oF6GgMa9+h9GiK6dg/dWzvuY3+I0QG6elIP0NMcWXxErzREpG0FolLHR+9gCqIi5iFO3uSGb+D/B5azg/KZwy6jAAAAAElFTkSuQmCC";
    private int characterLimit = 2001;

    protected enum connectionStatuses {
        connected, disconnected, error
    }

    protected connectionStatuses connectionStatus = connectionStatuses.disconnected;
    String ip = "35.189.80.190";
    int port = 5678;
    private String pass = "i;<tc2%Otv(\\5B,w0f\\w9,Tw|8v|uK2;Amibjxy?F`68oh8}\\Y2S|(7V=L;8fd";
    final double version = 1.09;

    public static void main(String[] args) {
        try {
            new Client();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void paint(Graphics g) {
        try {
            this.textFiedBoundry = this.getHeight() - 70;
            g.setColor(cfg.getBackgroundColour());
            g.fillRect(0, 0, frame.getWidth(), frame.getHeight());
            g.setColor(cfg.getTextColour());
            g.setFont(font);
            textFeild(g);
            drawHistory(g);
        } catch (Exception e) {
        }

    }

    Client() throws IOException {
        frame = new JFrame();

        byte[] imageByteArray = Base64.getDecoder().decode(this.icon.getBytes("UTF-8"));
        ByteArrayInputStream bis = new ByteArrayInputStream(imageByteArray);
        BufferedImage bImage2 = ImageIO.read(bis);

        frame.setIconImage(bImage2);

        frame.setBackground(Color.black);
        frame.add(this);

        frame.setSize(900, 700);
        frame.setResizable(false);

        // setting start position of the frame
        frame.setLocationRelativeTo(null);

        // closing
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Title
        frame.setTitle("DISKORD V" + this.version);

        // set frame visibility
        frame.setVisible(true);

        frame.addKeyListener(this);
        addMouseHandler();

        userName = getComputerName();

        sub = previousText.size() - 1;
        cfg = Config.getInstance(Color.darkGray, Color.green, Color.white, 40);
        try {
            loadConfig();
        } catch (Exception e) {
        }
        saveConfig();
        font = new Font("Impact", Font.TRUETYPE_FONT, cfg.getFontSize());
        // previousText.add("connecting...");
        this.outputToConsole("connecting...");
        repaint();
        try {
            connectToServer();
            while (this.connectionStatus == connectionStatuses.disconnected) {
                Thread.sleep(100);
                switch (this.connectionStatus) {
                    case connected:
                        this.connectedToServer = true;
                        System.out.println("connected");
                        break;
                    case disconnected:
                        break;
                    case error:
                        throw new Exception("your connection was probably refused :/ sry lmao");
                }
            }

            // setup client after an update
            this.postUpdateSetup();
            System.out.println("post update setup was sucessful");
            // update the client
            this.update();
            while (!updateSuccess) {
                // do nothing
                Thread.sleep(100);
            }

            // request the message log
            this.readMessages(0, this.localNumOfSavedMessages);
            Thread.sleep(1000);
            oldestLoadedChunk = ArrayListToString(previousText);
            System.out.println("connected and ready to go bby");
            // send welcome message
            outputToConsole("connected, use !help to see a list of commands");

        } catch (Exception e) {
            this.serverConnectionError(e.toString());
        }

        repaint();
    }

    private void update() {
        System.out.println("updating");
        int min = 1000;
        int max = 9999;
        int random_int = (int) Math.floor(Math.random() * (max - min + 1) + min);
        this.ct.isUpdating(true, random_int);
        this.writeMessage("!UPDATE:" + random_int + "," + this.version);
    }

    private void postUpdateSetup() {
        System.out.println("doing post update setup");
        File setupFile = new File("setup1.bat");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // if there has been an update
        if (setupFile.exists()) {
            // delete the old version
            if (setupFile.delete()) {
                this.outputToConsole("updated");
            } else {
                this.outputToConsole("we tried to update but idk, it fucked itself so... AHAHAHAHAH LOSER");
            }
        }
    }

    private void connectToServer() throws IOException {
        System.out.println("trying to connect to server");
        loadServerInfo();
        this.sock = new Socket(this.ip, this.port);
        this.in = sock.getInputStream();
        this.bin = new BufferedReader(new InputStreamReader(in));
        this.writer = new PrintWriter(sock.getOutputStream(), true);
        this.ct = new ClientThread(this, this.sock);
        ct.start();
        this.writeMessage(this.pass);
    }

    private void disconnectFromServer() throws IOException {
        ct.interrupt();
        this.sock.close();
    }

    protected void serverConnectionError(String msg) {
        this.connectedToServer = false;
        outputToConsole("UH OH... STINKY, ERROR CONNECTING TO SERVER \n");
        outputToConsole(msg);
        outputToConsole("use !help to see a list of commands");
        repaint();
    }

    private void loadServerInfo() {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(serverDetailsFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            ArrayList<String> data = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                data.add(line);
            }
            bufferedReader.close();
            this.ip = data.get(0);
            this.port = Integer.valueOf(data.get(1));

        } catch (Exception ex) {
            System.out.println("error loading config");
            saveServerInfo();
            ex.printStackTrace();
        }
    }

    private void saveServerInfo() {
        File f = new File(serverDetailsFile);
        try {
            FileWriter fw = new FileWriter(f, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(this.ip);
            bw.newLine();
            bw.write(String.valueOf(this.port));
            bw.close();
        } catch (IOException xe) {
        }
    }

    private String getComputerName() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME")) {
            return env.get("COMPUTERNAME");
        } else if (env.containsKey("HOSTNAME")) {
            return env.get("HOSTNAME");
        } else {
            return "Unknown Computer";
        }
    }

    // keylistner
    @Override
    public void keyPressed(KeyEvent ke) {
        // System.out.println(ke.getKeyCode());
        if (textSelected == true) {

            switch (ke.getKeyCode()) {

                case 8:
                    // backspace
                    if (currentText.length() != 0) {
                        String temp = "";
                        if (cursorOffset == 0) {
                            temp = currentText.substring(0, currentText.length() - 1);
                        } else {
                            temp = currentText.substring(0, (currentText.length() - 1) - cursorOffset)
                                    + currentText.substring(currentText.length() - cursorOffset);
                        }
                        currentText = temp;
                    }
                    break;

                case 10:
                    // enter
                    if (!currentText.equals("")) {
                        if ((connectedToServer == true) && (!currentText.startsWith("!", 0))) {

                            writeMessage(userName + ": " + currentText);
                        }
                        if (currentText.startsWith("!", 0)) {
                            if (currentText.equals("!help")) {
                                outputToConsole("----commands----");
                                outputToConsole("!fontColour(r,g,b)");
                                outputToConsole("!backgroundColour(r,g,b)");
                                outputToConsole("!cursorColour(r,g,b)");
                                outputToConsole("!fontSize(<size>)");
                                outputToConsole("!serverInfo");
                                outputToConsole("!setIP(<ip address>)");
                                outputToConsole("!setPort(<port no>)");
                                outputToConsole("!connect");
                                outputToConsole("!connectedClients");
                            } else {
                                // colour commands
                                Pattern pattern;
                                Matcher m;
                                pattern = Pattern.compile(colourRegex);
                                m = pattern.matcher(currentText);
                                String message = "done";

                                if (m.matches() == true) {
                                    Color c = new Color(0);
                                    try {
                                        int r = Integer.valueOf(m.group(3));
                                        int g = Integer.valueOf(m.group(4));
                                        int b = Integer.valueOf(m.group(5));
                                        c = new Color(r, g, b);
                                    } catch (Exception e) {
                                        message = "command error";
                                    }

                                    switch (m.group(1)) {
                                        case "background":
                                            cfg.setBackgroundColour(c);
                                            break;
                                        case "font":
                                            cfg.setTextColour(c);
                                            break;
                                        case "cursor":
                                            cfg.setCursorColour(c);
                                            break;
                                        default:
                                            message = "command error";
                                    }
                                    outputToConsole(message);

                                } else {
                                    // font size commands
                                    pattern = Pattern.compile(fontSizeRegex);
                                    m = pattern.matcher(currentText);
                                    if (m.matches() == true) {
                                        try {
                                            cfg.setFontSize(Integer.valueOf(m.group(2)));
                                        } catch (Exception e) {
                                            message = "command error";
                                        }
                                        outputToConsole(message);
                                    } else {
                                        // show server info commands
                                        pattern = Pattern.compile(serverInfoRegex);
                                        m = pattern.matcher(currentText);
                                        if (m.matches() == true) {
                                            if (this.sock.isClosed()) {
                                                this.connectedToServer = false;
                                            } else {
                                                this.connectedToServer = true;
                                            }
                                            String info = "Server Info---->    IP: " + this.ip + "    port: "
                                                    + this.port
                                                    + "    connected: " + this.connectedToServer;
                                            this.outputToConsole(info);
                                            repaint();
                                        } else {
                                            // set server info commands
                                            pattern = Pattern.compile(setServerInfoRegex);
                                            m = pattern.matcher(currentText);
                                            if (m.matches() == true) {
                                                switch (m.group(1)) {
                                                    case "IP":
                                                        this.ip = m.group(2);
                                                        break;
                                                    case "Port":
                                                        this.port = Integer.valueOf(m.group(2));
                                                }
                                                this.saveServerInfo();
                                                outputToConsole("done");
                                            } else {
                                                pattern = Pattern.compile(reConnectRegex);
                                                m = pattern.matcher(currentText);
                                                if (m.matches() == true) {
                                                    try {
                                                        this.disconnectFromServer();
                                                        this.connectToServer();
                                                    } catch (IOException e) {
                                                        // TODO Auto-generated catch block
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    pattern = Pattern.compile(connectedUsersRegex);
                                                    m = pattern.matcher(currentText);
                                                    if (m.matches() == true) {
                                                        this.writeMessage("!connectedClients:");
                                                    } else {
                                                        writeMessage(userName + ": " + currentText);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                saveConfig();
                            }
                        }

                    }
                    this.personalMessageLog.add(0, currentText);
                    currentText = "";
                    cursorOffset = 0;
                    // repaint();
                    break;

                case 16:
                    // shift
                    break;

                case 20:
                    // caps lock
                    break;

                case 17:
                    // Ctrl
                    ctrl = true;
                    break;

                case 18:
                    // ALT
                    alt = true;
                    break;

                case 86:
                    // v
                    if (ctrl == true) {
                        // Create a Clipboard object using getSystemClipboard() method
                        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                        String paste = "";
                        try {
                            paste = String.valueOf(c.getData(DataFlavor.stringFlavor));
                        } catch (UnsupportedFlavorException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // Get data stored in the clipboard that is in the form of a string (text)
                        for (int i = 0; i < paste.length(); i++) {
                            if (this.currentText.length() < this.characterLimit) {
                                currentText += paste.charAt(i);
                            }
                        }
                    } else {
                        writeKey(ke);
                    }
                    break;

                case 525:
                    // page key
                    break;

                case 39:
                    // right arrow
                    if (cursorOffset > 0) {
                        cursorOffset--;
                    }
                    break;

                case 37:
                    // left arrow
                    if (cursorOffset < currentText.length()) {
                        cursorOffset++;
                    }
                    break;

                case 38:
                    // up arrow
                    if (this.personalMessageLogIndex == 0) {
                        this.inProgressText = this.currentText;
                    }
                    if ((personalMessageLog.size() > 0)
                            && (this.personalMessageLogIndex < personalMessageLog.size())) {
                        this.currentText = this.personalMessageLog.get(personalMessageLogIndex);
                    }
                    if (this.personalMessageLogIndex < personalMessageLog.size()) {
                        this.personalMessageLogIndex++;
                    }
                    break;

                case 40:
                    // down arrow
                    if ((this.personalMessageLogIndex == personalMessageLog.size())
                            && (personalMessageLog.size() > 0)) {
                        this.personalMessageLogIndex--;
                    }
                    if (this.personalMessageLogIndex == 0) {
                        this.currentText = this.inProgressText;
                    } else {
                        this.personalMessageLogIndex--;
                        this.currentText = this.personalMessageLog.get(personalMessageLogIndex);
                    }
                    break;

                case 127:
                    // delete
                    delete = true;
                    break;

                case 192:
                    // backtick
                    if ((ctrl == true) && (alt == true)) {

                        System.exit(0);

                    }

                default:
                    writeKey(ke);
            }
            repaint();

        }

    }

    private void writeKey(KeyEvent ke) {
        if (currentText.length() < this.characterLimit) {
            if (cursorOffset == 0) {
                currentText += ke.getKeyChar();
            } else {
                String temp = currentText.substring(0, currentText.length() - cursorOffset) + ke.getKeyChar()
                        + currentText.substring(currentText.length() - cursorOffset);
                currentText = temp;
            }
        } else {
            // chararcter limit reached
        }
    }

    // adds a mouse listner to the program so the user can scroll thrugh chat
    private void addMouseHandler() {

        MouseInputAdapter mia = new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                System.out.println("mouse clicked");

                Point p = e.getPoint();

                int mouseX = p.x;
                int mouseY = p.y;

                if ((mouseX > 0) && (mouseX < frame.getWidth()) && (mouseY > textFiedBoundry)
                        && (mouseY < frame.getHeight())) {

                    System.out.println("textField");
                    if (textSelected == false) {

                        textSelected = true;

                    }

                } else {

                    textSelected = false;

                }
                repaint();

            }

        };
        frame.addMouseListener(mia);
        frame.addMouseMotionListener(mia);

        MouseWheelListener listener = new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent mwe) {

                int scroll = mwe.getWheelRotation();
                if (textSelected == false) {

                    if (scroll < 1) {

                        System.out.println("up");
                        sub--;
                        if (sub == -1) {
                            numOfExtraLoadedChunks++;
                            int numbOfBytes = oldestLoadedChunk.getBytes().length + 1;
                            ArrayList<String> nextChunk = new ArrayList<>();
                            try {
                                if (previousText.size() >= localNumOfSavedMessages) {
                                    ArrayList<String> tmp = new ArrayList<>(previousText.size());
                                    for (int i = 0; i < previousText.size(); i++) {
                                        tmp.add(previousText.get(i));
                                    }
                                    nextChunk = tmp;
                                    // previousText.clear();
                                    previousText = new ArrayList<>(previousText.size() + localNumOfSavedMessages);
                                    // nextChunk =
                                    readMessages(lastOffsetInBytes + numbOfBytes, localNumOfSavedMessages);
                                    // wait until all the older messages have been added to previous text
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

                                    // add the most recent messages to the end of the list
                                    for (String s : tmp) {
                                        previousText.add(s);
                                    }
                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            // ArrayList<String> temp = new ArrayList<>();
                            // temp.addAll(nextChunk);
                            // temp.addAll(previousText);
                            // previousText = temp;
                            lastOffsetInBytes += numbOfBytes;

                            oldestLoadedChunk = ArrayListToString(nextChunk);
                            // sub = (previousText.size() - (previousText.size() - nextChunk.size())) - 1;
                            sub = (previousText.size() - nextChunk.size()) - 1;
                            System.out.println("SUB: " + sub);
                            repaint();

                        }

                    } else {

                        System.out.println("down");
                        if (sub < previousText.size() - 1) {
                            sub++;
                            if (sub == previousText.size() - 1) {
                                try {
                                    if (previousText.size() >= localNumOfSavedMessages) {
                                        // previousText =
                                        readMessages(0, localNumOfSavedMessages);
                                    }
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                oldestLoadedChunk = ArrayListToString(previousText);
                                lastOffsetInBytes = 0;
                                numOfExtraLoadedChunks = 0;

                                sub = previousText.size() - 1;
                            }
                        }

                    }

                }
                repaint();

            }
        };

        frame.addMouseWheelListener(listener);
    }

    private void textFeild(Graphics g) {

        g.setColor(Color.white);
        g.drawRect(0, textFiedBoundry, this.getWidth(), this.getHeight());

        if (textSelected == true) {
            g.setColor(cfg.getTextColour());
            FontMetrics metrics = g.getFontMetrics(font);
            int adv = (metrics.stringWidth(currentText) + 4);
            // System.out.println(adv);
            g.setFont(font);
            int x = 5;
            if (adv > currentTextEndBoundry) {

                int move = (adv - currentTextEndBoundry);
                x = x - move - 2;
                adv = adv - move - 2;

            }
            g.drawString(currentText, x, this.getHeight() - 20);

            int offsetLength = 0;
            if (!currentText.isEmpty()) {
                String sub = currentText.substring(currentText.length() - cursorOffset, currentText.length());
                offsetLength = metrics.stringWidth(sub);
            }
            drawCursor(g, adv - offsetLength);

        }
        drawCharacterCounter(g, this.getWidth(), this.getHeight());
    }

    private void drawCharacterCounter(Graphics g, int x, int y) {
        String s = String.valueOf(this.currentText.length()) + "/" + String.valueOf(this.characterLimit);
        Font f = font.deriveFont((float) font.getSize() / 2);
        g.setColor(Color.white);
        g.setFont(f);
        FontMetrics metrics = g.getFontMetrics(f);
        x -= (metrics.stringWidth(s) + 5);
        y -= (metrics.getHeight());
        currentTextEndBoundry = x - 5;
        g.drawString(s, x, y);
        g.setFont(this.font);
    }

    private void drawCursor(Graphics g, int x) {

        g.setColor(cfg.getCursorColour());
        int y = textFiedBoundry + 4;
        int width = 3;
        int height = frame.getHeight() - textFiedBoundry;

        g.fillRect(x, y, width, height);

    }

    private void drawHistory(Graphics g) {
        if (!previousText.isEmpty()) {
            g.setColor(cfg.getTextColour());

            int x = 10;
            int y = textFiedBoundry - 10;
            FontMetrics metrics = g.getFontMetrics(font);
            int hgt = metrics.getHeight();

            for (int i = sub; i >= 0; i--) {
                int width = metrics.stringWidth(previousText.get(i));
                if (width > (frame.getWidth() - 100)) {
                    ArrayList<String> newMessage = wordWrap(previousText.get(i), metrics, frame.getWidth() - 100);
                    for (int j = newMessage.size() - 1; j >= 0; j--) {
                        String s = "";
                        if (j == 0) {
                            s += ">";
                        }
                        s += newMessage.get(j);
                        g.drawString(s, x, y);
                        y = y - hgt;
                    }
                } else {
                    g.drawString(">" + previousText.get(i), x, y);
                    y = y - hgt;
                }

            }

        }

    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    @Override
    public void keyReleased(KeyEvent ke) {
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
    }

    protected void outputToConsole(String outp) {
        if (sub == previousText.size() - 1) {
            int lastSubPos = sub;
            previousText.add(outp);
            if ((previousText.size() > localNumOfSavedMessages) && (numOfExtraLoadedChunks == 0)) {
                previousText.remove(0);
            }

            if (numOfExtraLoadedChunks > 0) {
                lastOffsetInBytes += (outp.length() + 2);
            }

            if (lastSubPos == previousText.size() - 2) {
                sub = previousText.size() - 1;
            }
            repaint();
        }
    }

    protected void writeMessage(String msg) {
        this.writer.println(msg);
    }

    private void readMessages(int offset, int buffer) throws IOException {
        System.out.println("requesting messages");
        ArrayList<String> messages = new ArrayList<String>();
        DataInputStream dis = new DataInputStream(this.sock.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(dis));
        // ask the server for the log request
        this.writer.println("LOGREQUEST" + offset + "," + buffer);

    }

    private String ArrayListToString(ArrayList<String> list) {

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                builder.append("\r\n");
            }
            builder.append(list.get(i));
        }
        return builder.toString();
    }

    private ArrayList<String> wordWrap(String msg, FontMetrics metrics, int limit) {
        // format string into arraylist
        ArrayList<String> words = new ArrayList<>();
        int cumulativeSum = 0;
        words = new ArrayList<String>(Arrays.asList(msg.split(" ")));
        ArrayList<Integer> positionsForNewline = new ArrayList<>();
        boolean addNewlines = true;
        boolean hasASpace = false;
        int startOfChunk = 0;
        int endOfChunk = 0;
        int lastSpace = 0;

        for (int i = 0; i < msg.length(); i++) {
            // TODO - see if character is a space
            if (msg.charAt(i) == ' ') {
                hasASpace = true;
                lastSpace = i;
            }
            if (metrics.stringWidth(msg.substring(startOfChunk, endOfChunk)) > (this.getWidth() - 40)) {
                if (hasASpace) {
                    // TODO - if theres a previous space set its position as the place to make a
                    // substring
                    positionsForNewline.add(lastSpace);
                    startOfChunk = lastSpace + 1;
                    hasASpace = false;
                } else {
                    // TODO - if theres no previous space then just break this current word in half
                    positionsForNewline.add(i - 1);
                    startOfChunk = i;
                }
            }
            endOfChunk++;
        }

        // while (addNewlines == true) {
        // cumulativeSum = 0;
        // int i;
        // try {
        // i = positionsForNewline.get(positionsForNewline.size() - 1);
        // } catch (IndexOutOfBoundsException e) {
        // i = 0;
        // }
        // while (i < words.size()) {
        // int wordSize = metrics.stringWidth(words.get(i));
        // cumulativeSum += wordSize;
        // if (cumulativeSum > limit) {
        // positionsForNewline.add(i - 1);
        // addNewlines = true;
        // break;
        // }
        // addNewlines = false;
        // i++;
        // }
        // }

        StringBuilder builder = new StringBuilder();
        startOfChunk = 0;
        for (int i : positionsForNewline) {
            builder.append(msg.substring(startOfChunk, i));
            builder.append("\n");
            startOfChunk = i;
        }
        builder.append(msg.substring(startOfChunk, msg.length()));

        // StringBuilder builder = new StringBuilder();
        // for (int j = 0; j < words.size(); j++) {
        // if (j != 0) {
        // builder.append(" ");
        // }
        // builder.append(words.get(j));
        // if (!positionsForNewline.isEmpty()) {
        // if (j == positionsForNewline.get(0)) {
        // builder.append("\n");
        // positionsForNewline.remove(0);
        // }
        // }
        // }

        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(builder.toString().split("\n")));
        return lines;

    }

    private void saveConfig() {

        File f = new File(configFile);
        try {
            FileWriter fw = new FileWriter(f, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(String.valueOf(cfg.getFontSize()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getAutoMessageColour().getRGB()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getBackgroundColour().getRGB()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getCursorColour().getRGB()));
            bw.newLine();
            bw.write(String.valueOf(cfg.getTextColour().getRGB()));
            bw.close();
        } catch (IOException xe) {
        }

    }

    private void loadConfig() {

        FileReader fileReader = null;
        try {
            fileReader = new FileReader(configFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            ArrayList<String> data = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                data.add(line);
            }
            cfg.setFontSize(Integer.valueOf(data.get(0)));
            Color c;
            c = new Color(Integer.valueOf(data.get(2)));
            cfg.setBackgroundColour(c);
            c = new Color(Integer.valueOf(data.get(3)));
            cfg.setCursorColour(c);
            c = new Color(Integer.valueOf(data.get(4)));
            cfg.setTextColour(c);

        } catch (Exception ex) {
            System.out.println("error loading config");
            ex.printStackTrace();
        }

    }
}

// TODO ----------------------------------------------------------------
/*
 * add support to copy
 * add encryption
 * add notification
 */
